package com.example.do_an.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnReset;
    private TextView tvBackToLogin;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_forgot_password);

        edtEmail = findViewById(R.id.edt_email);
        btnReset = findViewById(R.id.btn_reset);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);

        firebaseAuth = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(handleResetPassword);
        tvBackToLogin.setOnClickListener(v -> backToLoginScreen());
    }

    private final View.OnClickListener handleResetPassword = v -> {
        String email = edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Đã gửi email đặt lại mật khẩu đến " + email, Toast.LENGTH_LONG).show();

                        backToLoginScreen();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : "Lỗi không xác định";
                        Toast.makeText(this, "Gửi thất bại: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    };

    private void backToLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
