package com.example.do_an;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPasswordText, registerNowText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Khởi tạo view
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        registerNowText = findViewById(R.id.registerNowText);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Xử lý nút đăng nhập
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi Firebase Authentication để đăng nhập
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "Email hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // Mở trang Quên mật khẩu
        forgotPasswordText.setOnClickListener(v -> {
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Mở trang Đăng ký
        registerNowText.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
