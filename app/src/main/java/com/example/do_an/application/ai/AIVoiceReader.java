package com.example.do_an.application.ai;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class AIVoiceReader extends AppCompatActivity {
    private TextToSpeech tts;

    // 🔹 Constructor rỗng — Android cần cái này khi load Activity từ Manifest
    public AIVoiceReader() {
        // Không cần làm gì ở đây
    }

    // 🔹 Constructor có Context — dùng cho code bên ngoài gọi test
    public AIVoiceReader(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("vi", "VN"));
                tts.setSpeechRate(1.0f);
            }
        });
    }

    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
