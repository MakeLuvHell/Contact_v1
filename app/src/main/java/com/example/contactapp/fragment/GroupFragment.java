package com.example.contactapp.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactapp.R;
import com.example.contactapp.adapter.GroupAdapter;
import com.example.contactapp.model.Group;
import com.example.contactapp.viewmodel.ContactViewModel;
import com.example.contactapp.viewmodel.GroupViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/**
 * 分组Fragment类，用于显示联系人分组列表
 */
public class GroupFragment extends Fragment {
    private RecyclerView recyclerView;
    private GroupAdapter groupAdapter;
    private GroupViewModel groupViewModel;
    private ContactViewModel contactViewModel;
    private boolean useCardView;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        // 初始化 RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        useCardView = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean("card_view_mode", false);

        // 观察分组数据的变化
        groupViewModel.getAllGroups().observe(getViewLifecycleOwner(), this::updateGroupList);

        // 添加SharedPreferences变更监听器
        preferenceChangeListener = (sharedPreferences, key) -> {
            if (key.startsWith("group_display_")) {
                groupViewModel.getAllGroups().observe(getViewLifecycleOwner(), this::updateGroupList);
            }
        };
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // 设置添加分组按钮的点击事件
        FloatingActionButton fabAddGroup = view.findViewById(R.id.fab_add_group);
        fabAddGroup.setOnClickListener(v -> createInputDialog());

        return view;
    }

    private void createInputDialog() {
        // 创建输入对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("添加新分组");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("确认", (dialog, which) -> {
            String groupName = input.getText().toString().trim();
            if (!groupName.isEmpty()) {
                if (groupViewModel.checkIfGroupExists(groupName)) {
                    showGroupExistsDialog();
                } else {
                    Group newGroup = new Group(groupName);
                    groupViewModel.insertGroup(newGroup);
                    Toast.makeText(getContext(), "新分组已添加", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showGroupExistsDialog() {
        // 显示分组已存在的对话框
        new AlertDialog.Builder(getContext())
                .setTitle("分组已存在")
                .setMessage("该分组名称已存在，请选择其他名称。")
                .setPositiveButton("确认", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 移除SharedPreferences变更监听器
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    /**
     * 更新分组列表显示
     */
    private void updateGroupList(List<Group> groups) {
        List<Group> visibleGroups = new ArrayList<>();
        for (Group group : groups) {
            boolean isVisible = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean("group_display_" + group.getId(), true);
            if (isVisible) {
                visibleGroups.add(group);
            }
        }
        contactViewModel.getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
            groupAdapter = new GroupAdapter(getContext(), contactViewModel, groupViewModel, contacts, useCardView);
            recyclerView.setAdapter(groupAdapter);
            groupAdapter.submitList(visibleGroups);
        });
    }
}
