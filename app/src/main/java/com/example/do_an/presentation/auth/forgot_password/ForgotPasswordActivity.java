package com.example.do_an.presentation.auth.forgot_password;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.core.LocaleManager;
import com.example.do_an.core.constant.Language;
import com.example.do_an.data.common.LocalePreferences;
import com.example.do_an.presentation.auth.login.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private TextInputLayout textInputLayoutEmail;
    private MaterialButton btnReset;
    private View textBackToLogin;
    private Spinner spinnerLanguage;

    private LocalePreferences prefs;
    private boolean spinnerInitialized = false;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        prefs = new LocalePreferences(this);

        setupViews();
        loadViews();
        setupListeners();
    }

    private void setupViews() {
        editEmail = findViewById(R.id.editEmail);
        textInputLayoutEmail = findViewById(R.id.inputEmail);
        btnReset = findViewById(R.id.btnReset);
        textBackToLogin = findViewById(R.id.textBackToLogin);
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
        btnReset.setOnClickListener(v -> handleSendResetEmail());
        textBackToLogin.setOnClickListener(v -> navigateToLogin());
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

    private void handleSendResetEmail() {
        String email = editEmail.getText() != null
                ? editEmail.getText().toString().trim()
                : "";

        textInputLayoutEmail.setError(null);

        if (email.isEmpty()) {
            textInputLayoutEmail.setError(getString(R.string.error_empty_fields));
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        var message = getString(R.string.reset_email_sent) + " " + email;
                        showSuccess(message);
                        navigateToLogin();
                    } else {
                        String error = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_unknown);
                        var message = getString(R.string.reset_email_fail) + ": " + error;
                        showError(message);
                    }
                });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
