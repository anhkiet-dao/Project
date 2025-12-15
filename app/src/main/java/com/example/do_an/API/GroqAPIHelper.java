package com.example.do_an.API;

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

    public static String askAI(JSONArray messages) {

        HttpURLConnection conn = null;

        try {
            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", messages);
            body.put("temperature", 0.7);

            // ===== Kết nối =====
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            conn.setDoOutput(true);

            // ===== Gửi body =====
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

            // ===== Xử lý lỗi HTTP =====
            if (code == 401) {
                return "❌ API KEY không hợp lệ hoặc đã bị revoke";
            }

            if (code >= 400) {
                return "❌ Groq API lỗi " + code;
            }

            // ===== Parse kết quả =====
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
}
