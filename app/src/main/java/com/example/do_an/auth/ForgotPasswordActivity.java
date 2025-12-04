package com.example.do_an.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailEditText;
    private Button resetButton;
    private TextView backToLoginText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_forgot_password);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        emailEditText = findViewById(R.id.emailEditText);
        resetButton = findViewById(R.id.resetButton);
        backToLoginText = findViewById(R.id.backToLoginText);

        // Xử lý nút "Gửi liên kết đặt lại mật khẩu"
        resetButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gửi email đặt lại mật khẩu
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Đã gửi email đặt lại mật khẩu đến " + email,
                                    Toast.LENGTH_LONG).show();

                            // Quay lại màn hình đăng nhập
                            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                            Toast.makeText(ForgotPasswordActivity.this, "Gửi thất bại: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Quay lại màn hình đăng nhập
        backToLoginText.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
