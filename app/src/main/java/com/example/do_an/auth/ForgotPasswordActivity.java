package com.example.do_an.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputLayout emailLayout;
    private MaterialButton resetButton;
    private View backToLoginText;
    private android.widget.Spinner spinnerLanguage;
    private FirebaseAuth mAuth;
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadLocale(); // Áp dụng ngôn ngữ trước setContentView

        setContentView(R.layout.auth_activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        emailEditText = findViewById(R.id.emailEditText);
        emailLayout = findViewById(R.id.emailLayout);
        resetButton = findViewById(R.id.resetButton);
        backToLoginText = findViewById(R.id.backToLoginText);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        // Cập nhật text theo ngôn ngữ hiện tại
        updateTexts();

        // Thiết lập spinner chọn ngôn ngữ
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Languages, android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        // Chọn spinner theo ngôn ngữ hiện tại
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
                    updateTexts(); // Update text mà không recreate
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Gửi link reset password
        resetButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.reset_email_sent) + " " + email, Toast.LENGTH_LONG).show();
                            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            String error = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_unknown);
                            Toast.makeText(this, getString(R.string.reset_email_fail) + ": " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Quay lại Login
        backToLoginText.setOnClickListener(v -> {
            startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
            finish();
        });
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
        emailLayout.setHint(getString(R.string.email_hint));
        resetButton.setText(getString(R.string.reset_button));
        ((android.widget.TextView) backToLoginText).setText(getString(R.string.back_to_login));
        ((android.widget.TextView) findViewById(R.id.titleText)).setText(getString(R.string.forgot_password_title));
    }
}
