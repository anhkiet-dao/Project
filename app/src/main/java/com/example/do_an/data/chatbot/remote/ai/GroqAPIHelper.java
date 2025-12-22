package com.example.do_an.data.chatbot.remote.ai;

import android.util.Log;

import com.example.do_an.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GroqAPIHelper {

    private static final String TAG = "GroqAPI";
    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    /* ================= CALLBACK ================= */
    public interface GroqCallback {
        void onSuccess(String answer);
        void onError(String error);
    }

    /* ================= SYNC CALL (GIỮ NGUYÊN) ================= */
    public static String askAI(JSONArray messages) {

        HttpURLConnection conn = null;

        try {
            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", messages);
            body.put("temperature", 0.3);
            body.put("max_tokens", 500);

            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            code >= 200 && code < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream(),
                            "UTF-8"
                    )
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            Log.d(TAG, "HTTP " + code + " | " + response);

            if (code == 401) {
                return "❌ API KEY không hợp lệ";
            }

            if (code >= 400) {
                return "❌ Groq API lỗi " + code;
            }

            JSONObject json = new JSONObject(response.toString());
            return json
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (Exception e) {
            Log.e(TAG, "Exception", e);
            return "❌ Exception: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /* ================= ASYNC CALL (DÙNG TRONG UI) ================= */
    public static void askAIAsync(JSONArray messages, GroqCallback callback) {

        new Thread(() -> {
            String result = askAI(messages);

            if (result == null || result.startsWith("❌")) {
                callback.onError(
                        result != null ? result : "Không có phản hồi từ AI"
                );
            } else {
                callback.onSuccess(result);
            }
        }).start();
    }
}
