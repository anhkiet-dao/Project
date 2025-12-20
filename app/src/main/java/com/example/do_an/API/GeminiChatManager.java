package com.example.do_an.API;

import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GeminiChatManager {
    private final GenerativeModelFutures model;
    private final ExecutorService executorService;
    private final OkHttpClient client; // Thêm OkHttpClient

    public GeminiChatManager(String apiKey) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
        this.model = GenerativeModelFutures.from(gm);
        this.executorService = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient(); // Khởi tạo client mạng
    }

    // --- CASE 1: Đọc từ File (Offline) ---
    public void askAboutLocalPdf(String question, String filePath, String storyTitle, AIResponseCallback callback) {
        executorService.submit(() -> {
            File file = new File(filePath);
            if (!file.exists()) {
                callback.onError("Không tìm thấy file truyện.");
                return;
            }
            if (file.length() > 20 * 1024 * 1024) {
                callback.onError("File quá lớn (>20MB).");
                return;
            }

            try {
                byte[] pdfBytes = readFileToBytes(file);
                sendToGemini(pdfBytes, question, storyTitle, callback);
            } catch (Exception e) {
                callback.onError("Lỗi đọc file: " + e.getMessage());
            }
        });
    }

    // --- CASE 2: Đọc từ URL (Online) ---
    // --- CASE 2: Đọc từ URL (Online) ---
    public void askAboutOnlinePdf(String question, String pdfUrl, String storyTitle, AIResponseCallback callback) {
        executorService.submit(() -> {
            try {
                Request request = new Request.Builder().url(pdfUrl).build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError("Lỗi tải trang: " + response.code());
                        return;
                    }

                    long contentLength = response.body().contentLength();

                    // --- SỬA LỖI LOGIC ---
                    // Nếu contentLength = -1 (không xác định) hoặc lớn hơn 20MB
                    if (contentLength > 20 * 1024 * 1024) {
                        callback.onError("File quá lớn (>20MB).");
                        return;
                    }

                    // Nếu server không trả về kích thước (-1), ta cần cẩn trọng.
                    // Ở đây ta chấp nhận rủi ro hoặc chặn luôn nếu muốn an toàn tuyệt đối.
                    // Cách an toàn hơn: Dùng peekBody để kiểm tra nhưng phức tạp.
                    // Tạm thời: Nếu -1 vẫn cho tải nhưng cảnh báo log.
                    if (contentLength == -1) {
                        Log.w("GeminiChat", "Không xác định được kích thước file, có nguy cơ OOM nếu file quá lớn.");
                    }
                    // ---------------------

                    byte[] pdfBytes = response.body().bytes();

                    // Kiểm tra lại lần cuối sau khi tải xong (cho trường hợp contentLength == -1)
                    if (pdfBytes.length > 20 * 1024 * 1024) {
                        callback.onError("File tải về quá lớn (>20MB), AI từ chối xử lý.");
                        return;
                    }

                    sendToGemini(pdfBytes, question, storyTitle, callback);
                }
            } catch (IOException | OutOfMemoryError e) { // Bắt thêm lỗi OutOfMemoryError
                callback.onError("Lỗi tải/đọc file (Mạng hoặc Bộ nhớ): " + e.getMessage());
            }
        });
    }

    // --- LOGIC CHUNG: Gửi Byte lên Gemini ---
    private void sendToGemini(byte[] pdfBytes, String question, String storyTitle, AIResponseCallback callback) {
        Content content = new Content.Builder()
                .addBlob("application/pdf", pdfBytes)
                .addText("Đây là nội dung truyện: '" + storyTitle + "'. Trả lời câu hỏi: " + question)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (result != null && result.getText() != null) {
                    callback.onResponse(result.getText());
                } else {
                    callback.onError("AI không phản hồi.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                String msg = t.getMessage();
                if (msg != null && msg.contains("400")) {
                    callback.onError("Nội dung không hợp lệ hoặc bị chặn.");
                } else {
                    callback.onError("Lỗi AI: " + msg);
                }
            }
        }, executorService);
    }

    private byte[] readFileToBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) bos.write(buffer, 0, len);
            return bos.toByteArray();
        }
    }

    public interface AIResponseCallback {
        void onResponse(String answer);
        void onError(String error);
    }
}