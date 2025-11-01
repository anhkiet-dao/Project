package com.example.do_an;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvGender, tvBirthDate, tvPhone, tvEmail;
    private Button btnLogout;

    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ view
        tvFullName = findViewById(R.id.tvFullName);
        tvGender = findViewById(R.id.tvGender);
        tvBirthDate = findViewById(R.id.tvBirthDate);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        btnLogout = findViewById(R.id.btnLogout);

        // Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        databaseRef = FirebaseDatabase
                .getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userId);

        // Hiển thị email luôn (trường hợp chưa có thông tin khác)
        tvEmail.setText(currentUser.getEmail());

        // Lấy dữ liệu người dùng từ Firebase Realtime Database
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String birthDate = snapshot.child("birthDate").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);

                    // Nếu null thì hiển thị mặc định
                    tvFullName.setText(fullName != null ? fullName : "Chưa cập nhật");
                    tvGender.setText(gender != null ? gender : "Chưa cập nhật");
                    tvBirthDate.setText(birthDate != null ? birthDate : "Chưa cập nhật");
                    tvPhone.setText(phone != null ? phone : "Chưa cập nhật");

                } else {
                    Toast.makeText(ProfileActivity.this, "Không tìm thấy dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý đăng xuất
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }
}
