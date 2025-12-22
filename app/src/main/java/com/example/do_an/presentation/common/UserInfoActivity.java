package com.example.do_an.presentation.common;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.do_an.MainActivity;
import com.example.do_an.R;
import com.example.do_an.core.constant.FirebaseConstants;
import com.example.do_an.core.utils.Encryption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private TextInputEditText editFullName, editPhone, editBirthDate, editInterest;
    private TextInputLayout inputFullName, inputPhone, inputBirthDate, inputInterest;
    private RadioGroup radioGroupGender;
    private RadioButton radioMale, radioFemale;
    private MaterialButton btnSave;
    private ImageView imageAvatar;

    private Uri imageUri;
    private DatabaseReference databaseRef;
    private String userId, email;

    private ActivityResultLauncher<String> pickImageLauncher;
    private static final int REQUEST_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_user_info);

        setupLaunchers();
        if (!checkIncomingIntent()) {
            return;
        }

        databaseRef = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
                .getReference(FirebaseConstants.USERS_PATH);
        requestImagePermission();

        setupViews();
        setupListeners();
    }

    private void setupLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imageAvatar.setImageURI(uri);
                    }
                });
    }

    private boolean checkIncomingIntent() {
        userId = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (userId == null || email == null) {
            showError(getString(R.string.toast_missing_account));
            finish();
            return false;
        }
        return true;
    }

    private void setupViews() {
        imageAvatar = findViewById(R.id.imageAvatar);

        inputFullName = findViewById(R.id.inputFullName);
        inputPhone = findViewById(R.id.inputPhone);
        inputBirthDate = findViewById(R.id.inputBirthDate);
        inputInterest = findViewById(R.id.inputInterest);

        editFullName = findViewById(R.id.editFullName);
        editPhone = findViewById(R.id.editPhone);
        editBirthDate = findViewById(R.id.editBirthDate);
        editInterest = findViewById(R.id.editInterest);

        radioGroupGender = findViewById(R.id.radioGroupGender);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);

        btnSave = findViewById(R.id.btnSave);
    }

    private void setupListeners() {
        imageAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        editBirthDate.setOnClickListener(v -> showDateDialog());
        inputBirthDate.setEndIconOnClickListener(v -> showDateDialog());
        btnSave.setOnClickListener(v -> handleSaveUserInfo());
    }

    private void requestImagePermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE);
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
                    editBirthDate.setText(date);
                },
                y, m, d);
        dialog.show();
    }

    private void handleSaveUserInfo() {
        String fullName = editFullName.getText() != null ? editFullName.getText().toString().trim() : "";
        String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";
        String birthDate = editBirthDate.getText() != null ? editBirthDate.getText().toString().trim() : "";
        String interest = editInterest.getText() != null ? editInterest.getText().toString().trim() : "";

        inputFullName.setError(null);
        inputPhone.setError(null);
        inputBirthDate.setError(null);

        boolean isValid = true;

        if (fullName.isEmpty()) {
            inputFullName.setError(getString(R.string.error_empty_fields));
            isValid = false;
        }

        if (phone.isEmpty()) {
            inputPhone.setError(getString(R.string.error_empty_fields));
            isValid = false;
        }

        if (birthDate.isEmpty()) {
            inputBirthDate.setError(getString(R.string.error_empty_fields));
            isValid = false;
        }

        if (!isValid)
            return;

        int selectedId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedId);

        if (selectedGender == null) {
            showError(getString(R.string.toast_missing_info));
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

        // Process Image
        if (imageUri != null) {
            try (InputStream is = getContentResolver().openInputStream(imageUri)) {
                Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                map.put("avatarBase64", encodedImage);

            } catch (IOException e) {
                showError(getString(R.string.toast_image_error));
            }
        }

        // Save to Firebase
        databaseRef.child(userId).setValue(map)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(UserInfoActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showError(getString(R.string.toast_save_failed) + ": " + e.getMessage());
                });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
