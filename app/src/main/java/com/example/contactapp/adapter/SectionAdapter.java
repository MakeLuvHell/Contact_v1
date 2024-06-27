package com.example.contactapp.adapter;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.contactapp.R;
import com.example.contactapp.activity.ContactDetailActivity;
import com.example.contactapp.model.Contact;
import com.example.contactapp.viewmodel.ContactViewModel;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 分段适配器类，用于显示带有分段标题的联系人列表
 */
public class SectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_CARD = 2;
    private final Context context;
    private final ContactViewModel contactViewModel;
    private final List<Object> itemsWithHeaders;
    private final boolean useCardView;

    /**
     * 构造函数，初始化适配器
     * @param context 上下文对象
     * @param contactViewModel 联系人视图模型
     * @param useCardView 是否使用卡片视图
     */
    public SectionAdapter(Context context, ContactViewModel contactViewModel, boolean useCardView) {
        this.context = context;
        this.contactViewModel = contactViewModel;
        this.itemsWithHeaders = new ArrayList<>();
        this.useCardView = useCardView;
    }

    /**
     * 提交带有分段标题的联系人列表
     * @param list 联系人列表
     */
    public void submitListWithHeaders(List<Contact> list) {
        itemsWithHeaders.clear();

        char lastHeader = '\0';

        for (Contact contact : list) {
            char headerChar = getHeaderChar(contact.getName());
            String header;
            if (Character.isDigit(headerChar) || !Character.isLetter(headerChar)) {
                header = "#";
            } else {
                header = String.valueOf(headerChar).toUpperCase(Locale.getDefault());
            }

            if (headerChar != lastHeader) {
                itemsWithHeaders.add(header);
                lastHeader = headerChar;
            }

            itemsWithHeaders.add(contact);
        }

        notifyDataSetChanged();
    }

    /**
     * 获取联系人姓名的首字母
     * @param name 联系人姓名
     * @return 首字母字符
     */
    private char getHeaderChar(String name) {
        if (name == null || name.isEmpty()) {
            return '#';
        }
        char firstChar = name.charAt(0);
        if (Character.isLetter(firstChar)) {
            if (Character.toString(firstChar).matches("[\\u4e00-\\u9fa5]+")) { // 检查是否为汉字
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(firstChar);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    return Character.toUpperCase(pinyinArray[0].charAt(0));
                }
            } else {
                return Character.toUpperCase(firstChar);
            }
        }
        return '#';
    }

    @Override
    public int getItemViewType(int position) {
        if (itemsWithHeaders.get(position) instanceof String) {
            return TYPE_HEADER;
        } else {
            return useCardView ? TYPE_CARD : TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == TYPE_CARD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_card, parent, false);
            return new ContactViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);
            return new ContactViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) itemsWithHeaders.get(position));
        } else if (holder instanceof ContactViewHolder) {
            ((ContactViewHolder) holder).bind((Contact) itemsWithHeaders.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return itemsWithHeaders.size();
    }

    /**
     * 分段标题视图持有者类，用于绑定视图和数据
     */
    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerTextView;

        /**
         * 构造函数，初始化视图持有者
         * @param itemView 视图对象
         */
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTextView = itemView.findViewById(R.id.header_text);
        }

        /**
         * 绑定分段标题数据到视图
         * @param header 分段标题
         */
        public void bind(String header) {
            headerTextView.setText(header);
        }
    }

    /**
     * 联系人视图持有者类，用于绑定视图和数据
     */
    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView phoneTextView;
        private final ImageView contactImageView;

        /**
         * 构造函数，初始化视图持有者
         * @param itemView 视图对象
         */
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.contact_name);
            phoneTextView = itemView.findViewById(R.id.contact_phone);
            contactImageView = itemView.findViewById(R.id.contact_image);

            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = (Contact) itemsWithHeaders.get(position);
                    Intent intent = new Intent(context, ContactDetailActivity.class);
                    intent.putExtra("CONTACT_ID", contact.getId());
                    context.startActivity(intent);
                }
            });

            itemView.setOnLongClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = (Contact) itemsWithHeaders.get(position);
                    animateItemView(itemView, true);
                    showDeleteConfirmationDialog(contact);
                }
                return true;
            });
        }

        /**
         * 绑定联系人数据到视图
         * @param contact 联系人对象
         */
        public void bind(Contact contact) {
            // 设置联系人姓名和电话
            nameTextView.setText(contact.getName());
            phoneTextView.setText(contact.getPhone());

            Log.d("SectionAdapter", "Contact Name: " + contact.getName() + ", PhotoUri: " + contact.getPhotoUri());

            // 检查联系人是否有图片URI
            if (contact.getPhotoUri() != null) {
                // 解析图片URI
                Uri photoUri = Uri.parse(contact.getPhotoUri());
                Log.d("SectionAdapter", "Photo URI: " + photoUri.toString());

                // 创建文件对象，检查文件是否存在
                File file = new File(photoUri.getPath());
                if (file.exists()) {
                    // 使用Glide加载图片文件到ImageView
                    Glide.with(context)
                            .load(file)
                            .apply(RequestOptions.circleCropTransform())
                            .into(contactImageView);
                } else {
                    // 如果文件不存在，使用默认图片
                    contactImageView.setImageResource(R.drawable.ic_default);
                }
            } else {
                // 如果没有图片URI，使用默认图片
                contactImageView.setImageResource(R.drawable.ic_default);
            }
        }

        /**
         * 显示删除确认对话框
         * @param contact 联系人对象
         */
        private void showDeleteConfirmationDialog(Contact contact) {
            new AlertDialog.Builder(context)
                    .setTitle("删除联系人")
                    .setMessage("确定要删除该联系人？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        animateItemView(itemView, false); // 动画效果恢复正常
                        contactViewModel.delete(contact);
                        Toast.makeText(itemView.getContext(), "联系人已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", (dialog, which) -> animateItemView(itemView, false)) // 动画效果恢复正常
                    .show();
        }

        /**
         * 为长按事件添加动画效果
         * @param view    要动画的视图
         * @param enlarge 是否放大视图
         */
        private void animateItemView(View view, boolean enlarge) {
            float scale = enlarge ? 1.1f : 1.0f;
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", scale);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", scale);
            scaleX.setDuration(300);
            scaleY.setDuration(300);
            scaleX.start();
            scaleY.start();
        }
    }

    /**
     * 获取特定分段的位置
     * @param section 分段字符
     * @return 位置索引
     */
    public int getPositionForSection(char section) {
        for (int i = 0; i < itemsWithHeaders.size(); i++) {
            Object item = itemsWithHeaders.get(i);
            if (item instanceof String && ((String) item).charAt(0) == section) {
                return i;
            }
        }
        return -1;
    }
}
