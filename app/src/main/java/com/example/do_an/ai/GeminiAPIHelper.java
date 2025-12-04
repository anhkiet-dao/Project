package com.example.do_an.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class GeminiAPIHelper extends AppCompatActivity {

    private static final String API_KEY = "AIzaSyDUM3flAsx4pioO0xknBhxybXpxN208Vt0"; // 🔑 thay bằng key thật
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

    public static String askGemini(String prompt) {
        try {
            // ✅ Escape JSON an toàn: thay " và \ bằng ký tự hợp lệ
            prompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            // ✅ Tạo body JSON đúng cú pháp UTF-8
            String jsonBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + prompt + "\" } ] } ] }";

            // Kết nối
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            InputStream stream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            Log.d("GeminiAPI", "Response: " + response);

            // ✅ Parse JSON an toàn
            JSONObject json = new JSONObject(response.toString());
            if (!json.has("candidates")) {
                return "⚠️ API không trả về nội dung (HTTP " + code + ")";
            }

            JSONArray candidates = json.getJSONArray("candidates");
            JSONObject first = candidates.getJSONObject(0);
            JSONObject content = first.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");

            return parts.getJSONObject(0).getString("text");

        } catch (Exception e) {
            Log.e("GeminiAPI", "Lỗi khi gọi Gemini", e);
            return "❌ Lỗi khi gọi Gemini: " + e.getMessage();
        }
    }
}
