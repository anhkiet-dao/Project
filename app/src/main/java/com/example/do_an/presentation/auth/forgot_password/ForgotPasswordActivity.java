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
    private TextInputLayout inputEmail;
    private MaterialButton btnReset;
    private View textBackToLogin;
    private Spinner spinnerLanguage;

    private FirebaseAuth auth;
    private LocalePreferences localePrefs;

    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_forgot_password);

        bindViews();
        initDependencies();
        setupUi();
        bindActions();
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews() {
        editEmail = findViewById(R.id.editEmail);
        inputEmail = findViewById(R.id.inputEmail);
        btnReset = findViewById(R.id.btnReset);
        textBackToLogin = findViewById(R.id.textBackToLogin);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void initDependencies() {
        auth = FirebaseAuth.getInstance();
        localePrefs = new LocalePreferences(this);
    }

    private void setupUi() {
        setupLanguageSpinner();
        clearErrors();
    }

    private void bindActions() {
        btnReset.setOnClickListener(v -> onResetPasswordIntent());
        textBackToLogin.setOnClickListener(v -> onBackToLoginIntent());

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onLanguageSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void setupLanguageSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.Languages,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        Language current = localePrefs.getLanguage();
        spinnerLanguage.setSelection(current.getPosition());
    }

    private void clearErrors() {
        inputEmail.setError(null);
    }

    private String getText(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void showLoading(boolean show) {
        btnReset.setEnabled(!show);
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onResetPasswordIntent() {
        clearErrors();

        String email = getText(editEmail);

        if (!validateInput(email)) {
            return;
        }

        doSendResetEmail(email);
    }

    private void onBackToLoginIntent() {
        navigateToLogin();
    }

    private void onLanguageSelected(int position) {
        if (!spinnerInitialized) {
            spinnerInitialized = true;
            return;
        }

        Language selected = Language.fromPosition(position);
        if (selected != localePrefs.getLanguage()) {
            localePrefs.setLanguage(selected);
            spinnerLanguage.setSelection(selected.getPosition());
            LocaleManager.setLocale(selected);
        }
    }

    // =========================================================
    // 4️⃣ Validation
    // =========================================================

    private boolean validateInput(String email) {
        if (email.isEmpty()) {
            inputEmail.setError(getString(R.string.error_empty_fields));
            return false;
        }

        return true;
    }

    // =========================================================
    // 5️⃣ Business actions
    // =========================================================

    private void doSendResetEmail(String email) {
        showLoading(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (isFinishing()) return;

                    showLoading(false);

                    if (task.isSuccessful()) {
                        onResetEmailSuccess(email);
                    } else {
                        onResetEmailFailed(task.getException());
                    }
                });
    }

    // =========================================================
    // 6️⃣ Result handlers
    // =========================================================

    private void onResetEmailSuccess(String email) {
        String message = getString(R.string.reset_email_sent) + " " + email;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void onResetEmailFailed(Exception exception) {
        String error = exception != null
                ? exception.getMessage()
                : getString(R.string.error_unknown);
        String message = getString(R.string.reset_email_fail) + ": " + error;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // =========================================================
    // 7️⃣ Navigation
    // =========================================================

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
