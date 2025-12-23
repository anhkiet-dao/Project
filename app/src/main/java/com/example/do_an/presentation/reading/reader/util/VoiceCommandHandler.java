package com.example.do_an.presentation.reading.reader.util;

import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.presentation.reading.reader.PdfViewerController;

public class VoiceCommandHandler {

    private final VoiceCommandCallback callback;

    public VoiceCommandHandler(VoiceCommandCallback callback) {
        this.callback = callback;
    }

    public void handleCommand(String command) {
        String cmd = command.toLowerCase().trim();

        if (cmd.contains("mở ghi chú") || cmd.contains("Thêm ghi chú")) {
            callback.onOpenNote();
        } else if (cmd.contains("đóng ghi chú")) {
            callback.onCloseNote();
        } else if (cmd.contains("toàn màn hình") && !callback.isFullScreen()) {
            callback.onToggleFullScreen();
        } else if (cmd.contains("thoát toàn màn hình") && callback.isFullScreen()) {
            callback.onToggleFullScreen();
        } else if (cmd.contains("tiếp") || cmd.contains("Sau")) {
            callback.onNextPage();
        } else if (cmd.contains("trước")) {
            callback.onPreviousPage();
        } else if (cmd.contains("tải xuống") || cmd.contains("tải về") || cmd.contains("download")) {
            callback.onDownload();
        } else if (cmd.contains("bỏ yêu thích") || cmd.contains("xóa yêu thích")) {
            if (callback.isFavorite()) {
                callback.onToggleFavorite();
            }
        } else if (cmd.contains("yêu thích")) {
            if (!callback.isFavorite()) {
                callback.onToggleFavorite();
            }
        } else if (cmd.contains("quay lại")) {
            callback.onBack();
        }
    }

    public interface VoiceCommandCallback {
        void onOpenNote();

        void onCloseNote();

        void onToggleFullScreen();

        void onNextPage();

        void onPreviousPage();

        void onDownload();

        void onToggleFavorite();

        void onBack();

        boolean isFullScreen();

        boolean isFavorite();
    }
}
