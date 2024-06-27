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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.contactapp.R;
import com.example.contactapp.activity.ContactDetailActivity;
import com.example.contactapp.model.Contact;
import com.example.contactapp.viewmodel.ContactViewModel;

import java.io.File;

/**
 * 联系人适配器类，用于显示联系人列表
 */
public class ContactAdapter extends ListAdapter<Contact, ContactAdapter.ContactViewHolder> {
    private final Context context;
    private final ContactViewModel contactViewModel;
    private final boolean useCardView;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_CARD = 2;

    /**
     * 构造函数，初始化适配器
     * @param context 上下文对象
     * @param contactViewModel 联系人视图模型
     * @param useCardView 是否使用卡片视图
     */
    public ContactAdapter(Context context, ContactViewModel contactViewModel, boolean useCardView) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.contactViewModel = contactViewModel;
        this.useCardView = useCardView;
    }

    // 定义用于计算差异的回调
    private static final DiffUtil.ItemCallback<Contact> DIFF_CALLBACK = new DiffUtil.ItemCallback<Contact>() {
        @Override
        public boolean areItemsTheSame(@NonNull Contact oldItem, @NonNull Contact newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Contact oldItem, @NonNull Contact newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public int getItemViewType(int position) {
        return useCardView ? TYPE_CARD : TYPE_ITEM;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建并返回 ContactViewHolder
        View view;
        if (viewType == TYPE_CARD) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_card, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact_list, parent, false);
        }
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        // 绑定联系人数据到视图
        Contact currentContact = getItem(position);
        holder.bind(currentContact);
    }

    /**
     * 联系人视图持有者类，用于绑定视图和数据
     */
    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView, phoneTextView;
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

            // 设置点击事件，打开联系人详情
            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = getItem(position);
                    Intent intent = new Intent(context, ContactDetailActivity.class);
                    intent.putExtra("CONTACT_ID", contact.getId());
                    context.startActivity(intent);
                }
            });

            // 设置长按事件，显示删除确认对话框
            itemView.setOnLongClickListener(view -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Contact contact = getItem(position);
                    animateItemView(itemView, true);
                    showDeleteConfirmationDialog(contact);
                }
                return true;
            });
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

        /**
         * 绑定联系人数据到视图
         * @param contact 联系人对象
         */
        public void bind(Contact contact) {
            // 设置联系人姓名和电话
            nameTextView.setText(contact.getName());
            phoneTextView.setText(contact.getPhone());

            Log.d("ContactAdapter", "Contact Name: " + contact.getName() + ", PhotoUri: " + contact.getPhotoUri());

            // 检查联系人是否有图片URI
            if (contact.getPhotoUri() != null) {
                // 解析图片URI
                Uri photoUri = Uri.parse(contact.getPhotoUri());
                Log.d("ContactAdapter", "Photo URI: " + photoUri.toString());

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
    }
}
