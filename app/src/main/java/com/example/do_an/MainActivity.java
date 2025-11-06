package com.example.do_an;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.do_an.API.AIReadingAssistant;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import com.example.do_an.UI.Account;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    EditText edtQuestion;
    Button btnAsk;
    TextView tvResult;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Intent intent = new Intent(MainActivity.this, Account.class);
//        startActivity(intent);
//
//        // Kết thúc MainActivity để không quay lại
//        finish();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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


//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // ⚙️ Cho phép gọi API trực tiếp (chỉ nên dùng khi test)
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
//                .permitAll()
//                .build();
//        StrictMode.setThreadPolicy(policy);
//
//        // 🔗 Ánh xạ các View
//        edtQuestion = findViewById(R.id.edtQuestion);
//        btnAsk = findViewById(R.id.btnAsk);
//        tvResult = findViewById(R.id.tvResult);
//
//        tts = new TextToSpeech(this, status -> {
//            if (status == TextToSpeech.SUCCESS) {
//                int langResult = tts.setLanguage(new Locale("vi", "VN"));
//                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
//                    tvResult.setText("⚠️ Không hỗ trợ tiếng Việt trên thiết bị này.");
//                }
//            }
//        });
//
//        // 🧠 Xử lý khi bấm nút "Hỏi AI"
//        btnAsk.setOnClickListener(v -> {
//            String question = edtQuestion.getText().toString().trim();
//
//            if (question.isEmpty()) {
//                tvResult.setText("⚠️ Vui lòng nhập câu hỏi trước.");
//                return;
//            }
//
//            tvResult.setText("⏳ Đang hỏi AI, vui lòng chờ...");
//
//            // ✅ Dùng luồng riêng để không bị "NetworkOnMainThreadException"
//            new Thread(() -> {
//                try {
//                    // Gọi API Gemini qua lớp trợ lý
//                    String result = AIReadingAssistant.askAboutBook(question, "Cuốn Theo Chiều Gió");
//
//                    // Cập nhật UI phải làm trên luồng chính
//                    runOnUiThread(() -> tvResult.setText("📖 Trả lời của AI:\n" + result));
//
//                    if (tts != null) {
//                        tts.speak(result, TextToSpeech.QUEUE_FLUSH, null, null);
//                    }
//
//                } catch (Exception e) {
//                    runOnUiThread(() -> tvResult.setText("❌ Lỗi khi gọi AI: " + e.getMessage()));
//                }
//            }).start();
//        });
//    }
//
//    @Override
//    protected void onDestroy() {
//        // 🧹 Giải phóng tài nguyên TTS khi thoát app
//        if (tts != null) {
//            tts.stop();
//            tts.shutdown();
//        }
//        super.onDestroy();
//    }

}
