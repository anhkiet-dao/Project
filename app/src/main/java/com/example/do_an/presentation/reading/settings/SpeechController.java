package com.example.do_an.presentation.reading.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

public class SpeechController {
    private static final String TAG = "SpeechController";

    private Context context;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private SettingsManager settingsManager;
    private VoiceCommandListener currentListener;
    private boolean isDestroyed = false;

    public interface VoiceCommandListener {
        void onCommandRecognized(String command);
    }

    public SpeechController(Context context, SettingsManager settingsManager) {
        this.context = context;
        this.settingsManager = settingsManager;

        // Khởi tạo TTS
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
            }
        });

        initSpeechRecognizer();
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        } else {
            Log.e(TAG, "Speech recognition không hỗ trợ trên thiết bị này.");
        }
    }

    public void speak(String text) {
        if (!settingsManager.isVoiceControlEnabled() || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
    }

    public void startListening(VoiceCommandListener listener) {
        this.currentListener = listener;
        if (!settingsManager.isVoiceControlEnabled() || speechRecognizer == null || isDestroyed) return;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { Log.d(TAG, "Đang nghe..."); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Log.e(TAG, "Lỗi Speech: " + error);
                // Lỗi 7 là không nghe thấy gì, lỗi 8 là máy bận. Cần khởi động lại để nghe tiếp.
                restartListening();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    if (currentListener != null) {
                        currentListener.onCommandRecognized(command);
                    }
                }
                // Quan trọng: Sau khi có kết quả, đợi 1 chút rồi nghe tiếp
                restartListening();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void restartListening() {
        if (isDestroyed || !settingsManager.isVoiceControlEnabled()) return;

        // Trì hoãn một chút trước khi nghe lại để tránh xung đột audio
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (speechRecognizer != null && !isDestroyed) {
                speechRecognizer.cancel();
                startListening(currentListener);
            }
        }, 50);
    }

    public void stop() {
        if (speechRecognizer != null) speechRecognizer.stopListening();
        if (tts != null) tts.stop();
    }

    public void shutdown() {
        isDestroyed = true;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
    }
}