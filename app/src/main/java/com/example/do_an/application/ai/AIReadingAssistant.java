package com.example.do_an.application.ai;

import androidx.appcompat.app.AppCompatActivity;

public class AIReadingAssistant extends AppCompatActivity {

    public static String askAboutBook(String question, String bookTitle) {
        try {
            // ✅ Gộp câu hỏi thành prompt có ngữ cảnh rõ ràng
            String prompt = "Bạn là một trợ lý đọc sách thông minh, hiểu biết về văn học. "
                    + "Người dùng đang đọc cuốn sách có tên: \"" + bookTitle + "\". "
                    + "Vui lòng trả lời ngắn gọn, dễ hiểu cho câu hỏi sau: \"" + question + "\"";

            // ✅ Gọi Gemini API qua helper
            return GeminiAPIHelper.askGemini(prompt);

        } catch (Exception e) {
            return "❌ Lỗi khi tạo câu hỏi cho AI: " + e.getMessage();
        }
    }
}
