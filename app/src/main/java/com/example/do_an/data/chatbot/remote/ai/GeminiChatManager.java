package com.example.do_an.data.chatbot.remote.ai;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiChatManager {

    private static final String TAG = "GeminiChatManager";

    private final GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    /* ===== CALLBACK ===== */
    public interface Callback {
        void onSuccess(String answer);
        void onError(String error);
    }

    /* ===== CONSTRUCTOR ===== */
    public GeminiChatManager(@NonNull String apiKey) {

        // ⚠️ BẮT BUỘC PHẢI CÓ "models/"
        GenerativeModel generativeModel = new GenerativeModel(
                "models/gemini-1.0-pro",
                apiKey
        );

        model = GenerativeModelFutures.from(generativeModel);
    }

    /* ===== ASK GEMINI ===== */
    public void ask(@NonNull String prompt, @NonNull Callback callback) {

        Content content = new Content.Builder()
                .addText(prompt)
                .build();

        ListenableFuture<GenerateContentResponse> future =
                model.generateContent(content);

        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {

            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (result == null || result.getText() == null) {
                    callback.onError("Không có phản hồi từ AI");
                    return;
                }
                callback.onSuccess(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Gemini error", t);
                callback.onError(t.getMessage());
            }

        }, executor);
    }
}
