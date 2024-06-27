package com.example.contactapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.example.contactapp.R;
import com.example.contactapp.adapter.ViewPagerAdapter;
import com.example.contactapp.fragment.ContactFragment;
import com.example.contactapp.fragment.GroupFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private boolean isCardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupTheme();
        setupToolbar();
        setupViewPagerAndTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForThemeOrViewModeChange();
    }

    /**
     * 根据用户偏好设置使用的显示方式。
     */
    private void setupTheme() {
        isCardView = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("card_view_mode", false);
    }

    /**
     * 初始化并设置工具栏。
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("联系人");
    }

    /**
     * 初始化并设置 ViewPager 和 TabLayout。
     */
    private void setupViewPagerAndTabs() {
        TabLayout tabLayout = findViewById(R.id.tab_layout_test);
        ViewPager viewPager = findViewById(R.id.view_pager);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ContactFragment(), "联系人");
        adapter.addFragment(new GroupFragment(), "分组");

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * 检查用户偏好设置是否更改了视图模式，并重新创建活动以应用更改。
     */
    private void checkForThemeOrViewModeChange() {
        boolean currentView = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("card_view_mode", false);

        if (currentView != isCardView) {
            recreate();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_setting) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_add) {
            Intent intent = new Intent(this, ContactEditActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
