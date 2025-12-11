package com.example.do_an.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.MainActivity;
import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView forgotPasswordText, registerNowText, registerText;
    private Spinner spinnerLanguage;

    private FirebaseAuth mAuth;
    private boolean spinnerInitialized = false;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("userEmail", user.getEmail());
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadLocale(); // áp dụng ngôn ngữ trước setContentView

        setContentView(R.layout.auth_activity_login);

        // Ánh xạ view
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        registerNowText = findViewById(R.id.registerNowText);
        registerText = findViewById(R.id.registerText);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        mAuth = FirebaseAuth.getInstance();

        // Thiết lập spinner chọn ngôn ngữ
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String currentLang = prefs.getString("LANG", "vi");
        spinnerLanguage.setSelection(currentLang.equals("vi") ? 0 : 1);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerInitialized) {
                    spinnerInitialized = true;
                    return;
                }
                String selectedLang = position == 0 ? "vi" : "en";
                String savedLang = prefs.getString("LANG", "vi");
                if (!selectedLang.equals(savedLang)) {
                    setLocale(selectedLang);
                    updateTexts();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        updateTexts(); // cập nhật text theo ngôn ngữ hiện tại

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            getSharedPreferences("USER_PREF", MODE_PRIVATE)
                                    .edit()
                                    .putString("email", email)
                                    .putString("password", password)
                                    .apply();

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, MainActivity.class);
                                intent.putExtra("userEmail", user.getEmail());
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.login_fail), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        forgotPasswordText.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        registerNowText.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());

        getSharedPreferences("APP_PREFS", MODE_PRIVATE)
                .edit()
                .putString("LANG", lang)
                .apply();
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String lang = prefs.getString("LANG", "vi");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    private void updateTexts() {
        loginButton.setText(getString(R.string.login_button));
        forgotPasswordText.setText(getString(R.string.forgot_password));
        registerNowText.setText(getString(R.string.register_now));
        registerText.setText(getString(R.string.register_prompt));
        emailInput.setHint(getString(R.string.email_hint));
        passwordInput.setHint(getString(R.string.password_hint));
    }
}
