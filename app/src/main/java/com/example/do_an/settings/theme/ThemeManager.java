package com.example.do_an.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Quản lý theme (Light/Dark/System) cho ứng dụng
 */
public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final int MODE_SYSTEM = 0;  // Theo hệ thống
    public static final int MODE_LIGHT = 1;   // Luôn sáng
    public static final int MODE_DARK = 2;    // Luôn tối

    private final SharedPreferences prefs;

    public ThemeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Áp dụng theme đã lưu khi khởi động app
     */
    public void applyTheme() {
        int mode = getThemeMode();
        setThemeMode(mode);
    }

    /**
     * Lấy theme mode hiện tại
     */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Đặt và áp dụng theme mode
     * @param mode MODE_SYSTEM, MODE_LIGHT, hoặc MODE_DARK
     */
    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();

        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Kiểm tra có đang ở Dark Mode không
     */
    public boolean isDarkMode(Context context) {
        int nightMode = context.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Lấy tên theme mode để hiển thị
     */
    public String getThemeModeName(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return "Sáng";
            case MODE_DARK:
                return "Tối";
            case MODE_SYSTEM:
            default:
                return "Theo hệ thống";
        }
    }
}
