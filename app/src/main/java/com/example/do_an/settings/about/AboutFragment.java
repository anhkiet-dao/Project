package com.example.do_an.settings.hi;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Đã chuyển đổi từ AppCompatActivity sang Fragment
public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Tạo layout theo cách lập trình (giống như trong Activity cũ)

        // 1. Tạo Layout chính (LinearLayout)
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        // Chuyển đổi padding DP sang PX nếu cần, nhưng tạm thời giữ nguyên giá trị
        layout.setPadding(50, 50, 50, 50); // Giảm bớt padding top để hợp lý hơn trong Fragment

        // 2. Tạo TextView giới thiệu app
        TextView txtInfo = new TextView(getContext());
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

        // 3. Thêm TextView vào layout
        layout.addView(txtInfo);

        // Trả về layout đã tạo
        return layout;
    }
}