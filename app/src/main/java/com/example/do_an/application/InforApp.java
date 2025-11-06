package com.example.do_an.application;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class InforApp extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tạo layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 120, 50, 50);

        // Tạo TextView giới thiệu app
        TextView txtInfo = new TextView(this);
        txtInfo.setText(
                "📚 Chào mừng các độc giả đến với ứng dụng đọc Orumanga!\n\n" +
                        "Orumanga là ứng dụng giúp bạn đọc truyện tranh, manga một cách dễ dàng và tiện lợi.\n\n" +
                        "🔥 Tính năng nổi bật:\n" +
                        "• Kho truyện đa dạng, cập nhật liên tục.\n" +
                        "• Giao diện thân thiện, dễ thao tác.\n" +
                        "• Lưu trữ và đánh dấu chương yêu thích.\n" +
                        "• Hỗ trợ đọc offline mọi lúc mọi nơi.\n\n" +
                        "Hãy khám phá thế giới manga tuyệt vời ngay hôm nay và trải nghiệm cảm giác đọc truyện mượt mà, không gián đoạn!"
        );
        txtInfo.setTextSize(18);
        txtInfo.setTextColor(Color.BLACK);

        // Thêm TextView vào layout
        layout.addView(txtInfo);

        // Hiển thị layout
        setContentView(layout);
    }
}
