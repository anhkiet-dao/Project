package com.example.do_an.presentation.profile.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.core.LocaleManager;
import com.example.do_an.core.constant.Language;
import com.example.do_an.data.common.LocalePreferences;

public class SettingFragment extends Fragment {

    private View layoutLanguageSelector;
    private TextView textSelectedLanguage;
    private SwitchCompat switchDarkMode;
    private Button btnSave;

    private String[] languages;
    private String[] langCodes;
    private String selectedLangCode;
    private LocalePreferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupData();
    }

    private void setupData() {
        Language[] values = Language.values();
        languages = new String[values.length];
        langCodes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            languages[i] = values[i].getDisplayName();
            langCodes[i] = values[i].getCode();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_setting_en_vi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new LocalePreferences(requireContext());
        setupViews(view);
        loadViews();
        setupListeners();
    }

    private void setupViews(View view) {
        layoutLanguageSelector = view.findViewById(R.id.layoutLanguageSelector);
        textSelectedLanguage = view.findViewById(R.id.textSelectedLanguage);
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void loadViews() {
        Language currentLang = prefs.getLanguage();
        selectedLangCode = currentLang.getCode();
        textSelectedLanguage.setText(getLanguageName(selectedLangCode));

        // Load dark mode state
        boolean isDarkMode = isDarkModeEnabled();
        switchDarkMode.setChecked(isDarkMode);
    }

    private void setupListeners() {
        layoutLanguageSelector.setOnClickListener(v -> showLanguageDialog());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setDarkMode(isChecked);
        });

        btnSave.setOnClickListener(v -> {
            Language lang = Language.fromCode(selectedLangCode);
            if (lang != prefs.getLanguage()) {
                prefs.setLanguage(lang);
                LocaleManager.setLocale(lang);
            }
        });
    }

    private void showLanguageDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setSingleChoiceItems(languages, getSelectedIndex(), (dialog, which) -> {
                    selectedLangCode = langCodes[which];
                    textSelectedLanguage.setText(languages[which]);
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private int getSelectedIndex() {
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(selectedLangCode))
                return i;
        }
        return 0;
    }

    private String getLanguageName(String code) {
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(code))
                return languages[i];
        }
        return Language.VI.getDisplayName();
    }

    private boolean isDarkModeEnabled() {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        return prefs.getBoolean("dark_mode", false);
    }

    private void setDarkMode(boolean enabled) {
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", 0);
        prefs.edit().putBoolean("dark_mode", enabled).apply();

        int mode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
