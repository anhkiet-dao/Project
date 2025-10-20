package com.example.do_an;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Xử lý phần viền (status bar + navigation bar)
        View rootView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            // Lấy Insets thật (thanh trạng thái + thanh điều hướng)
            androidx.core.graphics.Insets systemBarsInsets =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Áp padding cho view để không bị che
            v.setPadding(
                    systemBarsInsets.left,
                    systemBarsInsets.top,
                    systemBarsInsets.right,
                    systemBarsInsets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
