package com.example.do_an.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.user.UserInfoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private Button registerButton;
    private TextView loginNowText, alreadyText, registerTitle;
    private Spinner spinnerLanguage;

    private FirebaseAuth auth;
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadLocale();

        setContentView(R.layout.auth_activity_register);

        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        confirmPasswordInput = findViewById(R.id.confirmPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginNowText = findViewById(R.id.loginNowText);
        alreadyText = findViewById(R.id.alreadyText);
        registerTitle = findViewById(R.id.registerTitle);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);

        auth = FirebaseAuth.getInstance();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("APP_PREFS", MODE_PRIVATE);
        String currentLang = prefs.getString("LANG", "vi");
        spinnerLanguage.setSelection(currentLang.equals("vi") ? 0 : 1);

        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
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
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        updateTexts();

        loginNowText.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, getString(R.string.password_not_match), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();

                        if (user != null) {
                            Intent intent = new Intent(RegisterActivity.this, UserInfoActivity.class);
                            intent.putExtra("uid", user.getUid());
                            intent.putExtra("email", user.getEmail());
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.error) + ": " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
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
        registerButton.setText(getString(R.string.register_button));
        alreadyText.setText(getString(R.string.already_have_account));
        loginNowText.setText(getString(R.string.login_now));
        registerTitle.setText(getString(R.string.register_title));
        emailInput.setHint(getString(R.string.email_hint));
        passwordInput.setHint(getString(R.string.password_hint));
        confirmPasswordInput.setHint(getString(R.string.confirm_password_hint));
    }
}
