package com.example.do_an.presentation.reading.reader;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class PdfOcrManager {

    private final TextRecognizer recognizer;

    // CALLBACK PHẢI NẰM TRONG CLASS
    public interface Callback {
        void onSuccess(String text);
        void onError(Exception e);
    }

    public PdfOcrManager() {
        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );
    }

    // METHOD ocr PHẢI PUBLIC
    public void ocr(Bitmap bitmap, Callback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(result ->
                        callback.onSuccess(result.getText()))
                .addOnFailureListener(callback::onError);
    }
}
