package com.example.do_an.presentation.auth.register;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.core.LocaleManager;
import com.example.do_an.core.constant.Language;
import com.example.do_an.data.common.LocalePreferences;
import com.example.do_an.data.common.UserPreferences;
import com.example.do_an.presentation.auth.login.LoginActivity;
import com.example.do_an.presentation.common.UserInfoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword, editConfirmPassword;
    private TextInputLayout textInputLayoutEmail, textInputLayoutPassword, textInputLayoutConfirmPassword;
    private MaterialButton btnRegister;
    private TextView textLoginNow;
    private Spinner spinnerLanguage;

    private FirebaseAuth mAuth;
    private LocalePreferences prefs;
    private UserPreferences userPrefs;
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

        mAuth = FirebaseAuth.getInstance();
        prefs = new LocalePreferences(this);
        userPrefs = new UserPreferences(this);

        setupViews();
        loadViews();
        setupListeners();
    }

    private void setupViews() {
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        textInputLayoutEmail = findViewById(R.id.inputEmail);
        textInputLayoutPassword = findViewById(R.id.inputPassword);
        textInputLayoutConfirmPassword = findViewById(R.id.inputConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        textLoginNow = findViewById(R.id.textLoginNow);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void loadViews() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        Language lang = prefs.getLanguage();
        spinnerLanguage.setSelection(lang.getPosition());
    }

    private void setupListeners() {
        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                handleSelectLanguage(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        textLoginNow.setOnClickListener(v -> navigateToLogin());
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleSelectLanguage(int position) {
        if (!spinnerInitialized) {
            spinnerInitialized = true;
            return;
        }

        Language selectedLang = Language.fromPosition(position);

        if (selectedLang != prefs.getLanguage()) {
            spinnerLanguage.setSelection(selectedLang.getPosition());
            LocaleManager.setLocale(selectedLang);
        }
    }

    private void handleRegister() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";
        String confirmPassword = editConfirmPassword.getText() != null
                ? editConfirmPassword.getText().toString().trim()
                : "";

        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);
        textInputLayoutConfirmPassword.setError(null);

        if (email.isEmpty()) {
            textInputLayoutEmail.setError(getString(R.string.error_empty_fields));
            return;
        }

        if (password.isEmpty()) {
            textInputLayoutPassword.setError(getString(R.string.error_empty_fields));
            return;
        }

        if (confirmPassword.isEmpty()) {
            textInputLayoutConfirmPassword.setError(getString(R.string.error_empty_fields));
            return;
        }

        if (!password.equals(confirmPassword)) {
            textInputLayoutConfirmPassword.setError(getString(R.string.password_not_match));
            return;
        }

        if (password.length() < 6) {
            textInputLayoutPassword.setError(getString(R.string.password_min_length));
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        showSuccess(getString(R.string.register_success));

                        if (user != null) {
                            userPrefs.saveUser(email, password);
                            navigateToUserInfo(user);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        showError(getString(R.string.error) + ": " + error);
                    }
                });
    }

    private void navigateToUserInfo(FirebaseUser user) {
        Intent intent = new Intent(RegisterActivity.this, UserInfoActivity.class);
        intent.putExtra("uid", user.getUid());
        intent.putExtra("email", user.getEmail());
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
