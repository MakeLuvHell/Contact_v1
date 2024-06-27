package com.example.contactapp.adapter;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactapp.R;
import com.example.contactapp.model.Contact;
import com.example.contactapp.model.Group;
import com.example.contactapp.viewmodel.ContactViewModel;
import com.example.contactapp.viewmodel.GroupViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分组适配器类，用于显示联系人分组列表
 */
public class GroupAdapter extends ListAdapter<Group, GroupAdapter.GroupViewHolder> {
    private final Context context;
    private final ContactViewModel contactViewModel;
    private final GroupViewModel groupViewModel;
    private final List<Contact> contactList;
    private final boolean useCardView;

    /**
     * 构造函数，初始化适配器
     * @param context 上下文对象
     * @param contactViewModel 联系人视图模型
     * @param groupViewModel 分组视图模型
     * @param contactList 联系人列表
     * @param useCardView 是否使用卡片视图
     */
    public GroupAdapter(Context context, ContactViewModel contactViewModel, GroupViewModel groupViewModel, List<Contact> contactList, boolean useCardView) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.contactViewModel = contactViewModel;
        this.groupViewModel = groupViewModel;
        this.contactList = contactList;
        this.useCardView = useCardView;
    }

    // 定义用于计算差异的回调
    private static final DiffUtil.ItemCallback<Group> DIFF_CALLBACK = new DiffUtil.ItemCallback<Group>() {
        @Override
        public boolean areItemsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Group oldItem, @NonNull Group newItem) {
            return oldItem.equals(newItem);
        }
    };

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 创建并返回 GroupViewHolder
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        // 绑定分组数据到视图
        Group currentGroup = getItem(position);
        holder.bind(currentGroup);
    }

    /**
     * 分组视图持有者类，用于绑定视图和数据
     */
    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupNameTextView;
        private final RecyclerView contactsRecyclerView;

        /**
         * 构造函数，初始化视图持有者
         * @param itemView 视图对象
         */
        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.group_name);
            contactsRecyclerView = itemView.findViewById(R.id.contacts_recycler_view);
        }

        /**
         * 绑定分组数据到视图
         * @param group 分组对象
         */
        public void bind(Group group) {
            // 将分组数据填充到视图中
            groupNameTextView.setText(group.getName());

            // 过滤当前组的联系人
            List<Contact> filteredContacts = contactList.stream()
                    .filter(contact -> contact.getGroupId() == group.getId())
                    .collect(Collectors.toList());

            // 创建联系人适配器并设置到RecyclerView
            ContactAdapter contactAdapter = new ContactAdapter(context, contactViewModel, useCardView);
            contactsRecyclerView.setAdapter(contactAdapter);
            contactsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
            contactAdapter.submitList(filteredContacts);

            // 设置点击展开逻辑
            itemView.setOnClickListener(v -> {
                if (contactsRecyclerView.getVisibility() == View.GONE) {
                    contactsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    contactsRecyclerView.setVisibility(View.GONE);
                }
            });

            // 设置长按删除逻辑
            itemView.setOnLongClickListener(v -> {
                animateItemView(itemView, true);
                new AlertDialog.Builder(context)
                        .setTitle("删除分组")
                        .setMessage("确定要删除该分组及其下的所有联系人吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            // 删除该分组及其下的所有联系人
                            animateItemView(itemView, false); // 动画效果恢复正常
                            deleteGroupAndContacts(group);
                            Toast.makeText(itemView.getContext(), "分组及其联系人已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", (dialog, which) -> animateItemView(itemView, false)) // 动画效果恢复正常
                        .show();
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
    }


    /**
     * 删除分组及其下的所有联系人
     * @param group 分组对象
     */
    private void deleteGroupAndContacts(Group group) {
        // 删除该分组下的所有联系人
        List<Contact> contactsToDelete = contactList.stream()
                .filter(contact -> contact.getGroupId() == group.getId())
                .collect(Collectors.toList());

        for (Contact contact : contactsToDelete) {
            contactViewModel.delete(contact);
        }

        // 删除分组
        groupViewModel.delete(group);

        // 从当前列表中移除
        List<Group> currentGroups = new ArrayList<>(getCurrentList());
        currentGroups.remove(group);
        submitList(currentGroups);
    }
}
