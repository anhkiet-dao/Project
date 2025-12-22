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
    private TextInputLayout textInputLayoutEmail, textInputLayoutPassword;
    private MaterialButton btnLogin;
    private TextView textForgotPassword, textRegister;
    private Spinner spinnerLanguage;

    private FirebaseAuth mAuth;
    private LocalePreferences prefs;
    private UserPreferences userPrefs;
    private boolean spinnerInitialized = false;

    @Override
    protected void onStart() {
        super.onStart();
        checkCurrentUser();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_login);

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
        textInputLayoutEmail = findViewById(R.id.inputEmail);
        textInputLayoutPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        textForgotPassword = findViewById(R.id.textForgotPassword);
        textRegister = findViewById(R.id.textRegister);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
    }

    private void loadViews() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.Languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        Language lang = prefs.getLanguage();
        spinnerLanguage.setSelection(lang.getPosition());

        // Populate email if saved
        String savedEmail = userPrefs.getEmail();
        if (savedEmail != null) {
            editEmail.setText(savedEmail);
        }
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

        btnLogin.setOnClickListener(v -> handleLogin());
        textForgotPassword.setOnClickListener(v -> navigateToForgotPassword());
        textRegister.setOnClickListener(v -> navigateToRegister());
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

    private void handleLogin() {
        String email = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";
        String password = editPassword.getText() != null ? editPassword.getText().toString().trim() : "";

        textInputLayoutEmail.setError(null);
        textInputLayoutPassword.setError(null);

        if (email.isEmpty()) {
            textInputLayoutEmail.setError(getString(R.string.error_empty_fields));
            return;
        }

        if (password.isEmpty()) {
            textInputLayoutPassword.setError(getString(R.string.error_empty_fields));
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            userPrefs.saveUser(email, password);
                            showSuccess(getString(R.string.login_success));
                            navigateToMain(user.getEmail());
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        showError(getString(R.string.login_fail) + ": " + error);
                    }
                });
    }

    private void checkCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            navigateToMain(user.getEmail());
        }
    }

    private void navigateToMain(String email) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("userEmail", email);
        startActivity(intent);
        finish();
    }

    private void navigateToForgotPassword() {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    private void navigateToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
