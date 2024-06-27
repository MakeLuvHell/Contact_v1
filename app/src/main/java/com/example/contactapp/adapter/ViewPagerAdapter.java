package com.example.contactapp.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * 视图分页适配器类，用于管理Fragment和它们的标题
 */
public class ViewPagerAdapter extends FragmentPagerAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();
    private final List<String> fragmentTitleList = new ArrayList<>();

    /**
     * 构造函数，初始化适配器
     * @param fm FragmentManager
     */
    public ViewPagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    /**
     * 获取指定位置的Fragment
     * @param position 位置索引
     * @return Fragment对象
     */
    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragmentList.get(position);
    }

    /**
     * 获取Fragment的数量
     * @return Fragment数量
     */
    @Override
    public int getCount() {
        return fragmentList.size();
    }

    /**
     * 添加Fragment及其标题
     * @param fragment Fragment对象
     * @param title Fragment的标题
     */
    public void addFragment(Fragment fragment, String title) {
        fragmentList.add(fragment);
        fragmentTitleList.add(title);
    }

    /**
     * 获取指定位置的页面标题
     * @param position 位置索引
     * @return 页面标题
     */
    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentTitleList.get(position);
    }
}
