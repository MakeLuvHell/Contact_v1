package com.example.contactapp.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.contactapp.R;
import com.example.contactapp.model.Contact;
import com.example.contactapp.model.Group;
import com.example.contactapp.viewmodel.ContactViewModel;
import com.example.contactapp.viewmodel.GroupViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ContactEditActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextInputEditText nameEditText, phoneEditText, emailEditText;
    private Spinner groupSpinner;
    private ImageView contactImageView;
    private Button saveContactButton;
    private ContactViewModel contactViewModel;
    private Contact contact;
    private Uri photoUri;
    private final List<Group> groupList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 根据用户偏好设置主题
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_edit);

        // 初始化视图
        initializeViews();
        setupToolbar();

        // 初始化 ViewModel
        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);
        GroupViewModel groupViewModel = new ViewModelProvider(this).get(GroupViewModel.class);

        loadContactDetails();
        loadGroupData(groupViewModel);
        setEventListeners();
    }

    /**
     * 通过ID查找视图
     */
    private void initializeViews() {
        nameEditText = findViewById(R.id.edit_name);
        phoneEditText = findViewById(R.id.edit_phone);
        emailEditText = findViewById(R.id.edit_email);
        groupSpinner = findViewById(R.id.edit_group_spinner);
        contactImageView = findViewById(R.id.contact_image);
        saveContactButton = findViewById(R.id.save_contact_button);
    }

    /**
     * 获取传递的联系人ID并加载详细信息
     */
    private void loadContactDetails() {
        int contactId = getIntent().getIntExtra("CONTACT_ID", -1);
        if (contactId != -1) {
            contactViewModel.getContactById(contactId).observe(this, contact -> {
                this.contact = contact;
                if (contact != null) {
                    populateContactData(contact);
                }
            });
        }
    }

    /**
     * 将联系人数据填充到视图中
     * 确保分组数据加载后匹配分组选项
     * @param contact 联系人实体
     */
    private void populateContactData(Contact contact) {
        nameEditText.setText(contact.getName());
        phoneEditText.setText(contact.getPhone());
        emailEditText.setText(contact.getEmail());

        if (!groupList.isEmpty()) {
            for (int i = 0; i < groupList.size(); i++) {
                if (groupList.get(i).getId() == contact.getGroupId()) {
                    groupSpinner.setSelection(i + 1);
                    break;
                }
            }
        }

        if (contact.getPhotoUri() != null) {
            photoUri = Uri.parse(contact.getPhotoUri());
            Glide.with(this)
                    .load(photoUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(contactImageView);
        }
    }

    /**
     * 从 ViewModel 加载分组数据并填充到 Spinner 中
     * 确保分组数据加载后，匹配选项
     * @param groupViewModel 获取all group的相关信息
     */
    private void loadGroupData(GroupViewModel groupViewModel) {
        groupViewModel.getAllGroups().observe(this, groups -> {
            groupList.clear();
            groupList.addAll(groups);
            List<String> groupNames = new ArrayList<>();
            groupNames.add("未分组");
            for (Group group : groups) {
                groupNames.add(group.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, groupNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupSpinner.setAdapter(adapter);

            if (contact != null) {
                for (int i = 0; i < groupList.size(); i++) {
                    if (groupList.get(i).getId() == contact.getGroupId()) {
                        groupSpinner.setSelection(i + 1); // 因为 "未分组" 是第一个选项，所以加1
                        break;
                    }
                }
            }
        });
    }

    /**
     * 设置点击事件选择图片
     * 设置保存按钮点击事件
     */
    private void setEventListeners() {
        contactImageView.setOnClickListener(view -> openImagePicker());
        saveContactButton.setOnClickListener(view -> saveContact());
    }

    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * 验证电话号是否符合规范
     * @param phone 电话号码
     * @return 是否符合规范
     */
    private boolean isValidPhone(String phone) {
        return Pattern.compile("^\\+?[0-9. ()-]{10,25}$").matcher(phone).matches();
    }

    /**
     * 验证邮件是否符合规范
     * @param email 邮件地址
     * @return 是否符合规范
     */
    private boolean isValidEmail(String email) {
        return Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$").matcher(email).matches();
    }

    /**
     * 保存联系人数据
     */
    private void saveContact() {
        String name = Objects.requireNonNull(nameEditText.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneEditText.getText()).toString().trim();
        String email = Objects.requireNonNull(emailEditText.getText()).toString().trim();

        if (name.isEmpty() || phone.isEmpty() ) {
            Toast.makeText(this, "请填写姓名和电话字段", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPhone(phone)) {
            Toast.makeText(this, "请输入有效的电话号码", Toast.LENGTH_SHORT).show();
            return;
        }

        if(!email.isEmpty()){
            if (!isValidEmail(email)) {
                Toast.makeText(this, "请输入有效的电子邮件地址", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        int selectedGroupId = -1;
        String selectedGroupName = "未分组";

        if (groupSpinner.getSelectedItemPosition() > 0) {
            selectedGroupId = groupList.get(groupSpinner.getSelectedItemPosition() - 1).getId();
            selectedGroupName = groupList.get(groupSpinner.getSelectedItemPosition() - 1).getName();
        }

        if (contact == null) {
            contact = new Contact(
                    name,
                    phone,
                    email,
                    selectedGroupId,
                    selectedGroupName,
                    photoUri != null ? photoUri.toString() : null
            );
            contactViewModel.insert(contact);
            Toast.makeText(getApplicationContext(), "新联系人已添加", Toast.LENGTH_SHORT).show();
        } else {
            updateContactData(name, phone, email, selectedGroupId, selectedGroupName);
            Toast.makeText(getApplicationContext(), "联系人信息已修改", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    /**
     * 更新联系人数据并保存到数据库
     * @param name 联系人姓名
     * @param phone 联系人电话
     * @param email 联系人邮件
     * @param selectedGroupId 联系人所在分组id
     * @param selectedGroupName 联系人所在分组名称
     */
    private void updateContactData(String name, String phone, String email, int selectedGroupId, String selectedGroupName) {
        contact.setName(name);
        contact.setPhone(phone);
        contact.setEmail(email);
        contact.setGroupId(selectedGroupId);
        contact.setGroup(selectedGroupName);
        contact.setPhotoUri(photoUri != null ? photoUri.toString() : null);
        contactViewModel.update(contact);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 检查请求代码和结果代码，以确保图片被成功选择
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // 获取选中图片的URI
            Uri imageUri = data.getData();
            // 保存图片到本地存储，并获取本地存储的URI
            photoUri = saveImageToInternalStorage(imageUri);
            // 使用Glide加载本地存储的图片到ImageView
            Glide.with(this)
                    .load(photoUri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(contactImageView);
        }
    }

    /**
     * 将图片保存到应用的内部存储，并返回本地存储的URI
     * @param imageUri 图片的Uri
     * @return 本地存储的Uri
     */
    private Uri saveImageToInternalStorage(Uri imageUri) {
        try {
            // 打开输入流读取选中的图片
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            // 创建一个文件用于存储图片
            File imageFile = new File(getFilesDir(), "contact_image_" + System.currentTimeMillis() + ".jpg");
            // 打开输出流将图片写入文件
            FileOutputStream outputStream = new FileOutputStream(imageFile);

            // 将图片从输入流写入输出流
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.close();
            inputStream.close();

            // 返回本地存储的图片URI
            return Uri.fromFile(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置工具栏
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_edit);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("编辑");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
    }
}
