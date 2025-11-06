package com.example.do_an.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.do_an.R;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchShowBottomBar, switchDarkMode;
    private Button btnClose;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Khởi tạo SharedPreferences
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Ánh xạ view
        switchShowBottomBar = findViewById(R.id.switch_show_bottom_bar);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        btnClose = findViewById(R.id.btnClose);

        // Đọc trạng thái hiện tại
        boolean showBottomBar = prefs.getBoolean("showBottomBar", true);
        boolean darkMode = prefs.getBoolean("darkMode", false);

        switchShowBottomBar.setChecked(showBottomBar);
        switchDarkMode.setChecked(darkMode);

        // Sự kiện bật/tắt thanh điều hướng
        switchShowBottomBar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("showBottomBar", isChecked).apply();
            // Gợi ý: có thể thêm callback để cập nhật UI ở activity chính
        });

        // Sự kiện đổi chế độ sáng/tối
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        // Nút đóng (trở về màn hình trước)
        btnClose.setOnClickListener(v -> finish());
    }
}
