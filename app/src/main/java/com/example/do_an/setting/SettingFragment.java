package com.example.do_an.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.do_an.MainActivity;
import com.example.do_an.R;

public class SettingFragment extends Fragment {

    private LinearLayout languageSelector;
    private TextView tvSelectedLanguage;
    private Button btnSaveLanguage;

    private String[] languages = {"Tiếng Việt", "English"};
    private String[] langCodes = {"vi", "en"};
    private String selectedLangCode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_setting_en_vi, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        languageSelector = view.findViewById(R.id.languageSelector);
        tvSelectedLanguage = view.findViewById(R.id.tvSelectedLanguage);
        btnSaveLanguage = view.findViewById(R.id.btnSaveLanguage);

        selectedLangCode = getSavedLocale();
        tvSelectedLanguage.setText(getLanguageName(selectedLangCode));

        languageSelector.setOnClickListener(v -> showLanguageDialog());

        btnSaveLanguage.setOnClickListener(v -> {
            saveLanguage(selectedLangCode);
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.updateAppLanguage(selectedLangCode); // Reload activity với locale mới
            }
        });
    }

    private void showLanguageDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Chọn ngôn ngữ")
                .setSingleChoiceItems(languages, getSelectedIndex(), (dialog, which) -> {
                    selectedLangCode = langCodes[which];
                    tvSelectedLanguage.setText(languages[which]);
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private int getSelectedIndex() {
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(selectedLangCode)) return i;
        }
        return 0;
    }

    private String getLanguageName(String code) {
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(code)) return languages[i];
        }
        return "Tiếng Việt";
    }

    private void saveLanguage(String langCode) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        prefs.edit().putString("App_Lang", langCode).apply();
    }

    private String getSavedLocale() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        return prefs.getString("App_Lang", "vi");
    }
}
