package com.example.do_an.API;

import androidx.appcompat.app.AppCompatActivity;

public class AISummarizer extends AppCompatActivity {
    public static String summarize(String chapterText) {
        String prompt = "Tóm tắt ngắn gọn chương sau bằng tiếng Việt, giữ ý chính:\n" + chapterText;
        return GeminiAPIHelper.askGemini(prompt);
    }

    public static String highlightPoints(String chapterText) {
        String prompt = "Liệt kê 3 ý chính quan trọng nhất trong đoạn sau:\n" + chapterText;
        return GeminiAPIHelper.askGemini(prompt);
    }
}