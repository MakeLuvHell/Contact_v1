package com.example.contactapp.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.contactapp.R;
import com.example.contactapp.adapter.SectionAdapter;
import com.example.contactapp.model.Contact;
import com.example.contactapp.view.SideLetterBar;
import com.example.contactapp.viewmodel.ContactViewModel;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 联系人Fragment类，用于显示联系人列表
 */
public class ContactFragment extends Fragment {
    private RecyclerView recyclerView;
    private SectionAdapter sectionAdapter;
    private ContactViewModel contactViewModel;
    private boolean useCardView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载 fragment_contact 布局
        View view = inflater.inflate(R.layout.fragment_contact, container, false);

        // 获取用户偏好设置，决定是否使用卡片视图
        useCardView = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean("card_view_mode", false);

        // 设置 RecyclerView
        setUpRecyclerView(view);
        return view;
    }

    private void setUpRecyclerView(View view) {
        // 初始化 RecyclerView 和布局管理器
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化 ViewModel 和适配器
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        sectionAdapter = new SectionAdapter(getContext(), contactViewModel, useCardView);
        recyclerView.setAdapter(sectionAdapter);

        // 设置搜索视图的查询监听器
        SearchView searchView = view.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false; // 不处理提交事件
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText); // 过滤联系人列表
                return true;
            }
        });

        // 初始化侧边字母栏
        SideLetterBar sideLetterBar = view.findViewById(R.id.side_letter_bar);
        TextView letterOverlay = view.findViewById(R.id.letter_overlay);
        letterOverlay.setVisibility(View.GONE); // 默认隐藏字母覆盖层
        sideLetterBar.setOverlay(letterOverlay);
        sideLetterBar.setOnLetterChangedListener(letter -> {
            // 根据字母滚动到相应位置
            int position = sectionAdapter.getPositionForSection(letter.charAt(0));
            if (position != -1) {
                ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).scrollToPositionWithOffset(position, 0);
            }
        });

        // 观察联系人数据变化
        contactViewModel.getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
            // 对联系人按拼音排序
            contacts.sort((c1, c2) -> {
                String name1 = getPinyin(c1.getName());
                String name2 = getPinyin(c2.getName());
                return name1.compareToIgnoreCase(name2);
            });
            // 提交排序后的联系人列表到适配器
            sectionAdapter.submitListWithHeaders(contacts);
        });
    }

    /**
     * 将字符串转换为拼音
     * @param input 输入字符串
     * @return 拼音字符串
     */
    private String getPinyin(String input) {
        StringBuilder pinyin = new StringBuilder();
        for (char c : input.toCharArray()) {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c);
            if (pinyinArray != null && pinyinArray.length > 0) {
                pinyin.append(pinyinArray[0]);
            } else {
                pinyin.append(c);
            }
        }
        return pinyin.toString();
    }

    /**
     * 过滤联系人列表
     * @param text 输入的过滤文本
     */
    private void filter(String text) {
        contactViewModel.getAllContacts().observe(getViewLifecycleOwner(), contacts -> {
            List<Contact> filteredList = contacts.stream()
                    .filter(contact -> contact.getName().toLowerCase().contains(text.toLowerCase()))
                    .collect(Collectors.toList());
            sectionAdapter.submitListWithHeaders(filteredList);
        });
    }
}
