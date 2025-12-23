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
    private TextInputLayout inputEmail, inputPassword, inputConfirmPassword;
    private MaterialButton btnRegister;
    private TextView textLoginNow;
    private Spinner spinnerLanguage;

    private FirebaseAuth auth;
    private LocalePreferences localePrefs;
    private UserPreferences userPrefs;

    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_register);

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
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        textLoginNow = findViewById(R.id.textLoginNow);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void initDependencies() {
        auth = FirebaseAuth.getInstance();
        localePrefs = new LocalePreferences(this);
        userPrefs = new UserPreferences(this);
    }

    private void setupUi() {
        setupLanguageSpinner();
        clearErrors();
    }

    private void bindActions() {
        btnRegister.setOnClickListener(v -> onRegisterIntent());
        textLoginNow.setOnClickListener(v -> onLoginIntent());

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
        inputPassword.setError(null);
        inputConfirmPassword.setError(null);
    }

    private String getText(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void showLoading(boolean show) {
        btnRegister.setEnabled(!show);
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onRegisterIntent() {
        clearErrors();

        String email = getText(editEmail);
        String password = getText(editPassword);
        String confirmPassword = getText(editConfirmPassword);

        if (!validateInput(email, password, confirmPassword)) {
            return;
        }

        doRegister(email, password);
    }

    private void onLoginIntent() {
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

    private boolean validateInput(String email, String password, String confirmPassword) {
        if (email.isEmpty()) {
            inputEmail.setError(getString(R.string.error_empty_fields));
            return false;
        }

        if (password.isEmpty()) {
            inputPassword.setError(getString(R.string.error_empty_fields));
            return false;
        }

        if (confirmPassword.isEmpty()) {
            inputConfirmPassword.setError(getString(R.string.error_empty_fields));
            return false;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError(getString(R.string.password_not_match));
            return false;
        }

        if (password.length() < 6) {
            inputPassword.setError(getString(R.string.password_min_length));
            return false;
        }

        return true;
    }

    // =========================================================
    // 5️⃣ Business actions
    // =========================================================

    private void doRegister(String email, String password) {
        showLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing()) return;

                    showLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            onRegisterSuccess(email, password, user);
                        }
                    } else {
                        onRegisterFailed(task.getException());
                    }
                });
    }

    // =========================================================
    // 6️⃣ Result handlers
    // =========================================================

    private void onRegisterSuccess(String email, String password, FirebaseUser user) {
        userPrefs.saveUser(email, password);
        Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
        navigateToUserInfo(user);
    }

    private void onRegisterFailed(Exception exception) {
        String error = exception != null ? exception.getMessage() : "Unknown error";
        String message = getString(R.string.error) + ": " + error;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // =========================================================
    // 7️⃣ Navigation
    // =========================================================

    private void navigateToUserInfo(FirebaseUser user) {
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(RegisterExtraConstant.USER_ID, user.getUid());
        i.putExtra(RegisterExtraConstant.EMAIL, user.getEmail());
        startActivity(i);
        finish();
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
