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

    private EditText edtFullName, edtPhone, edtBirthDate, edtInterest;
    private RadioGroup radioGender;
    private RadioButton radioMale, radioFemale;
    private Button btnSave;
    private ImageView imgAvatar;
    private TextView txtTitle, txtGender, txtBirthTitle;

    private Uri imageUri;

    private DatabaseReference databaseRef;
    private String userId, email;

    private ActivityResultLauncher<String> pickImageLauncher;
    private static final int REQUEST_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_user_info);

        // Ánh xạ view
        txtTitle = findViewById(R.id.txtTitle);
        txtGender = findViewById(R.id.txtGender);
        txtBirthTitle = findViewById(R.id.txtBirthDate);

        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        edtInterest = findViewById(R.id.edtinterest);

        radioGender = findViewById(R.id.radioGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        btnSave = findViewById(R.id.btnSave);
        imgAvatar = findViewById(R.id.imgAvatar);

        // Lấy thông tin từ Intent
        userId = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (userId == null || email == null) {
            Toast.makeText(this, getString(R.string.toast_missing_account), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        databaseRef = FirebaseDatabase.getInstance(
                "https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("Users");

        // Xin quyền ảnh
        requestImagePermission();

        // Launcher chọn ảnh
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imgAvatar.setImageURI(uri);
                    }
                }
        );

        imgAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        edtBirthDate.setOnClickListener(v -> showDateDialog());
        btnSave.setOnClickListener(v -> saveUserInfo());

        applyLocalizationText();
    }

    private void applyLocalizationText() {
        txtTitle.setText(getString(R.string.userinfo_title));
        txtGender.setText(getString(R.string.gender_label));
        txtBirthTitle.setText(getString(R.string.birthdate_label));

        radioMale.setText(getString(R.string.gender_male));
        radioFemale.setText(getString(R.string.gender_female));

        edtFullName.setHint(getString(R.string.hint_full_name));
        edtPhone.setHint(getString(R.string.hint_phone));
        edtBirthDate.setHint(getString(R.string.hint_birthdate));
        edtInterest.setHint(getString(R.string.hint_interest));

        btnSave.setText(getString(R.string.save_info));
    }

    private void requestImagePermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_CODE
                );
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE
                );
            }
        }
    }

    private void showDateDialog() {
        final Calendar calendar = Calendar.getInstance();
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                    edtBirthDate.setText(date);
                },
                y, m, d
        );
        dialog.show();
    }

    private void saveUserInfo() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String birthDate = edtBirthDate.getText().toString().trim();
        String interest = edtInterest.getText().toString().trim();

        int selectedId = radioGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedId);

        if (fullName.isEmpty() || phone.isEmpty() || birthDate.isEmpty() || selectedGender == null) {
            Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
            return;
        }

        String gender = selectedGender.getText().toString();

        Map<String, Object> map = new HashMap<>();
        map.put("fullName", Encryption.encrypt(fullName));
        map.put("phone", Encryption.encrypt(phone));
        map.put("birthDate", Encryption.encrypt(birthDate));
        map.put("gender", Encryption.encrypt(gender));
        map.put("interest", Encryption.encrypt(interest));
        map.put("email", Encryption.encrypt(email));

        // Xử lý ảnh
        if (imageUri != null) {
            try (InputStream is = getContentResolver().openInputStream(imageUri)) {

                Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                map.put("avatarBase64", encodedImage);

            } catch (IOException e) {
                Toast.makeText(this, getString(R.string.toast_image_error), Toast.LENGTH_SHORT).show();
            }
        }

        // Lưu Firebase
        databaseRef.child(userId).setValue(map)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UserInfoActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            getString(R.string.toast_save_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
