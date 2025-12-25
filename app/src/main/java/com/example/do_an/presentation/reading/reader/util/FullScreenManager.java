package com.example.do_an.presentation.reading.reader.util;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FullScreenManager {

    private final LinearLayout topBar;
    private final LinearLayout rootLayout;
    private final ImageView btnFullScreen;
    private final FullScreenCallback callback;

    private int originalPaddingLeft;
    private int originalPaddingTop;
    private int originalPaddingRight;
    private int originalPaddingBottom;

    private boolean isFullScreen = false;

    public FullScreenManager(LinearLayout topBar, LinearLayout rootLayout,
            ImageView btnFullScreen, FullScreenCallback callback) {
        this.topBar = topBar;
        this.rootLayout = rootLayout;
        this.btnFullScreen = btnFullScreen;
        this.callback = callback;

        // Save original padding values
        saveOriginalPadding();
    }

    private void saveOriginalPadding() {
        originalPaddingLeft = rootLayout.getPaddingLeft();
        originalPaddingTop = rootLayout.getPaddingTop();
        originalPaddingRight = rootLayout.getPaddingRight();
        originalPaddingBottom = rootLayout.getPaddingBottom();
    }

    public void toggle() {
        isFullScreen = !isFullScreen;

        if (isFullScreen) {
            enterFullScreen();
        } else {
            exitFullScreen();
        }
    }

    private void enterFullScreen() {
        topBar.setVisibility(View.GONE);
        rootLayout.setPadding(0, 0, 0, 0);
        callback.onFullScreenChanged(View.GONE);
        btnFullScreen.setImageResource(callback.getExitFullScreenIcon());
    }

    private void exitFullScreen() {
        topBar.setVisibility(View.VISIBLE);
        // Restore original padding values
        rootLayout.setPadding(
                originalPaddingLeft,
                originalPaddingTop,
                originalPaddingRight,
                originalPaddingBottom);
        callback.onFullScreenChanged(View.VISIBLE);
        btnFullScreen.setImageResource(callback.getEnterFullScreenIcon());
    }

    public boolean isFullScreen() {
        return isFullScreen;
    }

    public interface FullScreenCallback {
        void onFullScreenChanged(int bottomNavVisibility);

        int getEnterFullScreenIcon();

        int getExitFullScreenIcon();
    }
}
