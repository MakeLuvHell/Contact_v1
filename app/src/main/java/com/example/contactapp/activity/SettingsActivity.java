package com.example.contactapp.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.example.contactapp.R;
import com.example.contactapp.dao.ContactDao;
import com.example.contactapp.database.ContactDatabase;
import com.example.contactapp.model.Contact;
import com.example.contactapp.model.Group;
import com.example.contactapp.viewmodel.GroupViewModel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST_CODE = 1;
    private static final int REQUEST_WRITE_STORAGE = 112;

    private final List<Group> groupList = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    private ContactDao contactDao;
    private List<Contact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupToolbar();
        initContactDao();
        observeContacts();
        initGroupViewModel();
        setupViewModeSwitch();
        setupButtons();
    }

    /**
     * 初始化并设置工具栏。
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("设置");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * 初始化 ContactDao 实例。
     */
    private void initContactDao() {
        ContactDatabase db = ContactDatabase.getInstance(this);
        contactDao = db.contactDao();
    }

    /**
     * 观察联系人数据变化并更新本地列表。
     */
    private void observeContacts() {
        contactDao.getAllContacts().observe(this, contacts -> contactList = contacts);
    }

    /**
     * 初始化 GroupViewModel 并观察分组数据变化。
     */
    private void initGroupViewModel() {
        GroupViewModel groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        groupViewModel.getAllGroups().observe(this, groups -> {
            groupList.clear();
            groupList.addAll(groups);
        });
    }

    /**
     * 设置视图模式切换开关。
     */
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private void setupViewModeSwitch() {
        Switch viewModeSwitch = findViewById(R.id.switch_card_view);
        viewModeSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("card_view_mode", false));
        viewModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean("card_view_mode", isChecked).apply());
    }

    /**
     * 初始化按钮并设置点击事件。
     */
    private void setupButtons() {
        Button groupSettingsButton = findViewById(R.id.button_group_settings);
        groupSettingsButton.setOnClickListener(v -> showGroupSelectionDialog());

        Button exportContactButton = findViewById(R.id.button_export);
        exportContactButton.setOnClickListener(view -> checkStoragePermissionAndProceed(this::exportContacts));

        Button importContactButton = findViewById(R.id.button_import);
        importContactButton.setOnClickListener(view -> checkStoragePermissionAndProceed(this::openFilePicker));
    }

    /**
     * 显示分组选择对话框，允许用户选择要显示的分组。
     */
    private void showGroupSelectionDialog() {
        String[] groupNames = groupList.stream().map(Group::getName).toArray(String[]::new);
        boolean[] checkedItems = new boolean[groupList.size()];

        for (int i = 0; i < groupList.size(); i++) {
            checkedItems[i] = PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("group_display_" + groupList.get(i).getId(), true);
        }

        new AlertDialog.Builder(this)
                .setTitle("选择分组显示")
                .setMultiChoiceItems(groupNames, checkedItems, (dialog, which, isChecked) ->
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .edit().putBoolean("group_display_" + groupList.get(which).getId(), isChecked).apply())
                .setPositiveButton("确认", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * 导出联系人到文件。
     */
    private void exportContacts() {
        if (contactList == null) {
            Toast.makeText(this, "没有可导出的联系人", Toast.LENGTH_LONG).show();
            return;
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "contacts.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Contact contact : contactList) {
                writer.write(contact.toString());
                writer.newLine();
            }
            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
            Toast.makeText(this, "联系人已导出到：" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "导出失败", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * 打开文件选择器以选择要导入的文件。
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            importContacts(data.getData());
        }
        if (requestCode == REQUEST_WRITE_STORAGE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                exportContacts();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法导入或导出联系人", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 从文件导入联系人。
     * @param uri 文件的 Uri
     */
    private void importContacts(Uri uri) {
        executor.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Contact contact = Contact.fromString(line);
                    contactDao.insert(contact);
                }
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "联系人导入成功", Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(SettingsActivity.this, "导入失败", Toast.LENGTH_LONG).show());
                e.printStackTrace();
            }
        });
    }

    /**
     * 检查存储权限并进行导出或导入操作。
     * @param action 要执行的操作
     */
    private void checkStoragePermissionAndProceed(Runnable action) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                action.run();
            } else {
                showPermissionExplanationDialog(() -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivityForResult(intent, REQUEST_WRITE_STORAGE);
                });
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                action.run();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            }
        }
    }

    /**
     * 显示权限请求的解释对话框。
     * @param proceedAction 授权后的操作
     */
    private void showPermissionExplanationDialog(Runnable proceedAction) {
        new AlertDialog.Builder(this)
                .setTitle("存储权限请求")
                .setMessage("为了导入和导出联系人，应用需要访问您的存储。请授予存储权限。")
                .setPositiveButton("授予", (dialog, which) -> proceedAction.run())
                .setNegativeButton("拒绝", (dialog, which) -> {
                    dialog.dismiss();
                    Toast.makeText(this, "存储权限被拒绝，无法导入或导出联系人", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "存储权限被拒绝，无法导入或导出联系人", Toast.LENGTH_LONG).show();
            }
        }
    }
}
