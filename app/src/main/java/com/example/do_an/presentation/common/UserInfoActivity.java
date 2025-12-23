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

    private DatabaseReference databaseRef;
    private ActivityResultLauncher<String> pickImageLauncher;

    private Uri imageUri;
    private String userId, email;

    private static final int REQUEST_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity_user_info);

        bindViews();
        initDependencies();
        setupUi();
        bindActions();
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews() {
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

    private void initDependencies() {
        databaseRef = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
                .getReference(FirebaseConstants.USERS_PATH);

        setupImagePicker();
        extractIntentData();
    }

    private void setupUi() {
        requestImagePermission();
    }

    private void bindActions() {
        imageAvatar.setOnClickListener(v -> onSelectImageIntent());
        editBirthDate.setOnClickListener(v -> onSelectDateIntent());
        inputBirthDate.setEndIconOnClickListener(v -> onSelectDateIntent());
        btnSave.setOnClickListener(v -> onSaveUserInfoIntent());
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        imageAvatar.setImageURI(uri);
                    }
                });
    }

    private void extractIntentData() {
        userId = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (userId == null || email == null) {
            Toast.makeText(this, getString(R.string.toast_missing_account), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void requestImagePermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.READ_MEDIA_IMAGES },
                        REQUEST_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_PERMISSION_CODE);
            }
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    String date = d + "/" + (m + 1) + "/" + y;
                    editBirthDate.setText(date);
                },
                year, month, day);
        dialog.show();
    }

    private void clearErrors() {
        inputFullName.setError(null);
        inputPhone.setError(null);
        inputBirthDate.setError(null);
    }

    private String getText(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void showLoading(boolean show) {
        btnSave.setEnabled(!show);
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onSelectImageIntent() {
        pickImageLauncher.launch("image/*");
    }

    private void onSelectDateIntent() {
        showDatePicker();
    }

    private void onSaveUserInfoIntent() {
        clearErrors();

        String fullName = getText(editFullName);
        String phone = getText(editPhone);
        String birthDate = getText(editBirthDate);
        String interest = getText(editInterest);

        if (!validateInput(fullName, phone, birthDate)) {
            return;
        }

        String gender = getSelectedGender();
        if (gender == null) {
            Toast.makeText(this, getString(R.string.toast_missing_info), Toast.LENGTH_SHORT).show();
            return;
        }

        doSaveUserInfo(fullName, phone, birthDate, gender, interest);
    }

    // =========================================================
    // 4️⃣ Validation
    // =========================================================

    private boolean validateInput(String fullName, String phone, String birthDate) {
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

        return isValid;
    }

    private String getSelectedGender() {
        int selectedId = radioGroupGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedId);
        return selectedGender != null ? selectedGender.getText().toString() : null;
    }

    // =========================================================
    // 5️⃣ Business actions
    // =========================================================

    private void doSaveUserInfo(String fullName, String phone, String birthDate, String gender, String interest) {
        showLoading(true);

        Map<String, Object> userData = buildUserData(fullName, phone, birthDate, gender, interest);

        databaseRef.child(userId).setValue(userData)
                .addOnSuccessListener(a -> onSaveSuccess())
                .addOnFailureListener(e -> onSaveFailed(e));
    }

    private Map<String, Object> buildUserData(String fullName, String phone, String birthDate, String gender,
            String interest) {
        Map<String, Object> map = new HashMap<>();
        map.put("fullName", Encryption.encrypt(fullName));
        map.put("phone", Encryption.encrypt(phone));
        map.put("birthDate", Encryption.encrypt(birthDate));
        map.put("gender", Encryption.encrypt(gender));
        map.put("interest", Encryption.encrypt(interest));
        map.put("email", Encryption.encrypt(email));

        if (imageUri != null) {
            String encodedImage = encodeImage(imageUri);
            if (encodedImage != null) {
                map.put("avatarBase64", encodedImage);
            }
        }

        return map;
    }

    private String encodeImage(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.toast_image_error), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    // =========================================================
    // 6️⃣ Result handlers
    // =========================================================

    private void onSaveSuccess() {
        showLoading(false);
        Toast.makeText(this, getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
        navigateToMain();
    }

    private void onSaveFailed(Exception exception) {
        showLoading(false);
        String message = getString(R.string.toast_save_failed) + ": " + exception.getMessage();
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // =========================================================
    // 7️⃣ Navigation
    // =========================================================

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
