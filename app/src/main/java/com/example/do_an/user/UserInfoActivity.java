package com.example.do_an.user;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.*;
import android.content.Intent;

import com.example.do_an.MainActivity;
import com.example.do_an.R;
import com.example.do_an.application.Encryption;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private EditText edtFullName, edtPhone, edtBirthDate, edtinterest;
    private RadioGroup radioGender;
    private Button btnSave;
    private ImageView imgAvatar;
    private Uri imageUri;

    private DatabaseReference databaseRef;
    private String userId, email;

    private ActivityResultLauncher<String> pickImageLauncher;
    private static final int REQUEST_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        radioGender = findViewById(R.id.radioGender);
        btnSave = findViewById(R.id.btnSave);
        edtinterest = findViewById(R.id.edtinterest);
        imgAvatar = findViewById(R.id.imgAvatar);

        userId = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (userId == null || email == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Firebase Database
        databaseRef = FirebaseDatabase.getInstance(
                        "https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        // Xin quyền đọc ảnh
        requestImagePermission();

        // Chọn ảnh
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imgAvatar.setImageURI(uri);
                        Log.d("UserInfoActivity", "Đã chọn ảnh: " + uri);
                    }
                });

        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        edtBirthDate.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveUserInfo());
    }

    private void requestImagePermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
            }
        }
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    edtBirthDate.setText(date);
                },
                year, month, day
        );
        dialog.show();
    }

    private void saveUserInfo() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String birthDate = edtBirthDate.getText().toString().trim();
        String interest = edtinterest.getText().toString().trim();

        int selectedId = radioGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedId);
        String gender = selectedGender != null ? selectedGender.getText().toString() : "";

        if (fullName.isEmpty() || phone.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", Encryption.encrypt(fullName));
        userMap.put("gender", Encryption.encrypt(gender));
        userMap.put("birthDate", Encryption.encrypt(birthDate));
        userMap.put("phone", Encryption.encrypt(phone));
        userMap.put("email", Encryption.encrypt(email));
        userMap.put("interest", Encryption.encrypt(interest));

        // Nếu người dùng có chọn ảnh, convert thành Base64
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                userMap.put("avatarBase64", encodedImage);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi đọc ảnh!", Toast.LENGTH_SHORT).show();
            }
        }

        databaseRef.child(userId).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UserInfoActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserInfoActivity", "Lỗi ghi Firebase: " + e.getMessage());
                    Toast.makeText(this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
