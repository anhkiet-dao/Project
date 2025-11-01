package com.example.do_an;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import com.google.firebase.database.*;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private EditText edtFullName, edtPhone, edtBirthDate;
    private RadioGroup radioGender;
    private Button btnSave;

    private DatabaseReference databaseRef;
    private String userId, email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        // Ánh xạ view
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        radioGender = findViewById(R.id.radioGender);
        btnSave = findViewById(R.id.btnSave);

        // ✅ Nhận dữ liệu từ RegisterActivity
        userId = getIntent().getStringExtra("uid");
        email = getIntent().getStringExtra("email");

        if (userId == null || email == null) {
            Toast.makeText(this, "Không tìm thấy tài khoản. Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ✅ Khởi tạo đúng Firebase Database
        databaseRef = FirebaseDatabase.getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users");

        // Mở DatePicker khi bấm vào ô ngày sinh
        edtBirthDate.setOnClickListener(v -> showDatePickerDialog());

        // Lưu thông tin
        btnSave.setOnClickListener(v -> {
            Log.d("UserInfoActivity", "Nút Lưu được bấm");
            saveUserInfo();
        });
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

        int selectedId = radioGender.getCheckedRadioButtonId();
        RadioButton selectedGender = findViewById(selectedId);
        String gender = selectedGender != null ? selectedGender.getText().toString() : "";

        if (fullName.isEmpty() || phone.isEmpty() || birthDate.isEmpty() || gender.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gói dữ liệu người dùng
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullName", fullName);
        userMap.put("gender", gender);
        userMap.put("birthDate", birthDate);
        userMap.put("phone", phone);
        userMap.put("email", email);

        Log.d("UserInfoActivity", "Bắt đầu ghi dữ liệu Firebase...");

        // ✅ Ghi vào Firebase
        databaseRef.child(userId).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserInfoActivity", "Ghi dữ liệu thành công!");
                    Toast.makeText(UserInfoActivity.this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang MainActivity
                    Intent intent = new Intent(UserInfoActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("UserInfoActivity", "Lỗi ghi Firebase: " + e.getMessage());
                    Toast.makeText(UserInfoActivity.this, "Lưu thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
