package com.example.do_an.API;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.Executors;

public class GeminiChatManager {
    private final GenerativeModelFutures model;

    public GeminiChatManager(String apiKey) {
        // Sử dụng gemini-1.5-flash để tốc độ phản hồi nhanh hơn
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
        this.model = GenerativeModelFutures.from(gm);
    }

    public void askAboutLocalPdf(String question, String filePath, String storyTitle, AIResponseCallback callback) {
        File file = new File(filePath);

        // 1. Kiểm tra file có tồn tại không
        if (!file.exists()) {
            callback.onError("Không tìm thấy file truyện tại hệ thống.");
            return;
        }

        // 2. Kiểm tra kích thước file (Tránh lỗi OOM và giới hạn API)
        long fileSizeInBytes = file.length();
        long fileSizeInMB = fileSizeInBytes / (1024 * 1024);
        if (fileSizeInMB > 20) {
            callback.onError("Truyện quá lớn (" + fileSizeInMB + "MB). AI hiện chỉ hỗ trợ đọc tập dưới 20MB.");
            return;
        }

        // 3. Đọc và gửi dữ liệu
        try {
            // Đọc bytes từ đường dẫn
            byte[] pdfBytes = Files.readAllBytes(file.toPath());

            Content content = new Content.Builder()
                    .addBlob("application/pdf", pdfBytes)
                    .addText("Bạn là một chuyên gia về truyện. Đây là nội dung tập truyện: '" + storyTitle +
                            "'. Dựa vào toàn bộ nội dung file này, hãy trả lời câu hỏi của tôi: " + question)
                    .build();

            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (result != null && result.getText() != null) {
                        callback.onResponse(result.getText());
                    } else {
                        callback.onError("AI không thể trả lời câu hỏi này.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    callback.onError("Lỗi kết nối AI: " + t.getMessage());
                }
            }, Executors.newSingleThreadExecutor());

        } catch (Exception e) {
            callback.onError("Lỗi hệ thống: " + e.getMessage());
        }
    }

    public interface AIResponseCallback {
        void onResponse(String answer);
        void onError(String error);
    }
}