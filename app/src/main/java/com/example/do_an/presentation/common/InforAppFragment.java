package com.example.do_an.presentation.common;

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

public class InforAppFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.setPadding(50, 50, 50, 50);

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

        layout.addView(txtInfo);

        return layout;
    }
}