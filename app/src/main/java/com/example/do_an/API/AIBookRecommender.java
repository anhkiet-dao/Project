package com.example.do_an.API;

import androidx.appcompat.app.AppCompatActivity;

public class AIBookRecommender extends AppCompatActivity {
    public static String suggest(String lastBookTitle) {
        String prompt = "Người dùng vừa đọc xong cuốn '" + lastBookTitle
                + "'. Hãy gợi ý 3 cuốn sách tương tự hoặc có phong cách giống vậy.";
        return GeminiAPIHelper.askGemini(prompt);
    }
}
