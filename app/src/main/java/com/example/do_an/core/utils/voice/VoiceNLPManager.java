package com.example.do_an.core.utils.voice;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.Normalizer;
import java.util.ArrayList;

public class VoiceNLPManager {

    public enum VoiceIntent {
        NEXT_PAGE,
        PREVIOUS_PAGE,
        OPEN_FAVORITE,
        TOGGLE_FULLSCREEN,
        UNKNOWN
    }

    public interface VoiceCallback {
        void onCommandDetected(VoiceIntent intent, String rawText);
    }

    private static final int REQ_SPEECH = 1001;

    private Fragment fragment;
    private VoiceCallback callback;

    public VoiceNLPManager(Fragment fragment, VoiceCallback callback) {
        this.fragment = fragment;
        this.callback = callback;
    }

    /* ================= START ================= */

    public void startListening() {
        if (ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            fragment.requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQ_SPEECH
            );
        } else {
            openSpeech();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_SPEECH && resultCode == android.app.Activity.RESULT_OK && data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                String text = results.get(0);
                callback.onCommandDetected(detectIntent(normalize(text)), text);
            }
        }
    }

    private void openSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói lệnh");

        fragment.startActivityForResult(intent, REQ_SPEECH);
    }

    /* ================= NLP ================= */

    private String normalize(String text) {
        text = text.toLowerCase();
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return text.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private VoiceIntent detectIntent(String t) {
        if (t.contains("tiep") || t.contains("chuong sau")) {
            return VoiceIntent.NEXT_PAGE;
        }
        if (t.contains("truoc") || t.contains("quay lai")) {
            return VoiceIntent.PREVIOUS_PAGE;
        }
        if (t.contains("toan man hinh") || t.contains("fullscreen")) {
            return VoiceIntent.TOGGLE_FULLSCREEN;
        }
        if (t.contains("yeu thich")) {
            return VoiceIntent.OPEN_FAVORITE;
        }
        return VoiceIntent.UNKNOWN;
    }
}
