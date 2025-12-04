package com.example.do_an.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.example.do_an.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Dialog để chọn theme (Light/Dark/System)
 */
public class ThemeDialogHelper {

    public interface OnThemeSelectedListener {
        void onThemeSelected(int mode);
    }

    public static void showThemeDialog(Context context, OnThemeSelectedListener listener) {
        ThemeManager themeManager = new ThemeManager(context);
        int currentMode = themeManager.getThemeMode();

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_theme_selector, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupTheme);
        RadioButton radioSystem = dialogView.findViewById(R.id.radioSystem);
        RadioButton radioLight = dialogView.findViewById(R.id.radioLight);
        RadioButton radioDark = dialogView.findViewById(R.id.radioDark);

        // Set current selection
        switch (currentMode) {
            case ThemeManager.MODE_LIGHT:
                radioLight.setChecked(true);
                break;
            case ThemeManager.MODE_DARK:
                radioDark.setChecked(true);
                break;
            case ThemeManager.MODE_SYSTEM:
            default:
                radioSystem.setChecked(true);
                break;
        }

        new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setPositiveButton("Áp dụng", (dialog, which) -> {
                    int selectedMode;
                    int checkedId = radioGroup.getCheckedRadioButtonId();

                    if (checkedId == R.id.radioLight) {
                        selectedMode = ThemeManager.MODE_LIGHT;
                    } else if (checkedId == R.id.radioDark) {
                        selectedMode = ThemeManager.MODE_DARK;
                    } else {
                        selectedMode = ThemeManager.MODE_SYSTEM;
                    }

                    themeManager.setThemeMode(selectedMode);

                    if (listener != null) {
                        listener.onThemeSelected(selectedMode);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
