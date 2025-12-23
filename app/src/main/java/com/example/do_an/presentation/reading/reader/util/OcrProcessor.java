package com.example.do_an.presentation.reading.reader.util;

import android.graphics.Bitmap;
import android.widget.TextView;

import com.example.do_an.presentation.reading.reader.PdfOcrManager;

public class OcrProcessor {

    private final PdfOcrManager ocrManager;
    private final OcrCallback callback;

    public OcrProcessor(OcrCallback callback) {
        this.ocrManager = new PdfOcrManager();
        this.callback = callback;
    }

    public void processBitmap(Bitmap bitmap, TextView loadingText) {
        if (bitmap == null) {
            callback.onError("Không lấy được ảnh trang");
            return;
        }

        callback.onStart();
        if (loadingText != null) {
            loadingText.setText("Đang phân tích trang...");
        }

        ocrManager.ocr(bitmap, new PdfOcrManager.Callback() {
            @Override
            public void onSuccess(String text) {
                callback.onSuccess(text);
            }

            @Override
            public void onError(Exception e) {
                callback.onError("OCR thất bại");
            }
        });
    }

    public interface OcrCallback {
        void onStart();

        void onSuccess(String text);

        void onError(String message);
    }
}
