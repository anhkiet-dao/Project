package com.example.do_an.presentation.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.MainActivity;
import com.example.do_an.R;
import com.example.do_an.core.LocaleManager;
import com.example.do_an.core.constant.Language;
import com.example.do_an.data.common.LocalePreferences;
import com.example.do_an.data.common.UserPreferences;
import com.example.do_an.presentation.auth.forgot_password.ForgotPasswordActivity;
import com.example.do_an.presentation.auth.register.RegisterActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private TextInputLayout inputEmail, inputPassword;
    private MaterialButton btnLogin;
    private TextView textForgotPassword, textRegister;
    private Spinner spinnerLanguage;

    private FirebaseAuth auth;
    private LocalePreferences localePrefs;
    private UserPreferences userPrefs;

    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_login);

        bindViews();
        initDependencies();
        setupUi();
        bindActions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkCurrentUser();
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews() {
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textForgotPassword = findViewById(R.id.textForgotPassword);
        textRegister = findViewById(R.id.textRegister);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void initDependencies() {
        auth = FirebaseAuth.getInstance();
        localePrefs = new LocalePreferences(this);
        userPrefs = new UserPreferences(this);
    }

    private void setupUi() {
        setupLanguageSpinner();
        populateSavedEmail();
        clearErrors();
    }

    private void bindActions() {
        btnLogin.setOnClickListener(v -> onLoginIntent());
        textForgotPassword.setOnClickListener(v -> onForgotPasswordIntent());
        textRegister.setOnClickListener(v -> onRegisterIntent());

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

    private void populateSavedEmail() {
        String savedEmail = userPrefs.getEmail();
        if (savedEmail != null) {
            editEmail.setText(savedEmail);
        }
    }

    private void clearErrors() {
        inputEmail.setError(null);
        inputPassword.setError(null);
    }

    private String getText(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void showLoading(boolean show) {
        btnLogin.setEnabled(!show);
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onLoginIntent() {
        clearErrors();

        String email = getText(editEmail);
        String password = getText(editPassword);

        if (!validateInput(email, password)) {
            return;
        }

        doLogin(email, password);
    }

    private void onForgotPasswordIntent() {
        navigateToForgotPassword();
    }

    private void onRegisterIntent() {
        navigateToRegister();
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

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            inputEmail.setError(getString(R.string.error_empty_fields));
            return false;
        }

        if (password.isEmpty()) {
            inputPassword.setError(getString(R.string.error_empty_fields));
            return false;
        }

        return true;
    }

    // =========================================================
    // 5️⃣ Business actions
    // =========================================================

    private void doLogin(String email, String password) {
        showLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing()) return;

                    showLoading(false);

                    if (task.isSuccessful()) {
                        onLoginSuccess(email, password);
                    } else {
                        onLoginFailed(task.getException());
                    }
                });
    }

    private void checkCurrentUser() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            navigateToMain(user.getEmail());
        }
    }

    // =========================================================
    // 6️⃣ Result handlers
    // =========================================================

    private void onLoginSuccess(String email, String password) {
        userPrefs.saveUser(email, password);
        Toast.makeText(this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
        checkCurrentUser();
    }

    private void onLoginFailed(Exception exception) {
        String error = exception != null ? exception.getMessage() : "Unknown error";
        String message = getString(R.string.login_fail) + ": " + error;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // =========================================================
    // 7️⃣ Navigation
    // =========================================================

    private void navigateToMain(String email) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(LoginExtraConstant.EMAIL, email);
        startActivity(i);
        finish();
    }

    private void navigateToForgotPassword() {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }
}
