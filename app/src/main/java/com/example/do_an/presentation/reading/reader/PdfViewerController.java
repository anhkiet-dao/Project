package com.example.do_an.presentation.reading.reader;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.R;
import com.example.do_an.presentation.reading.reader.adapter.PdfPageAdapter;
import com.example.do_an.presentation.reading.settings.SettingsManager;

import java.io.File;

public class PdfViewerController {

    private final Context context;
    private ViewPager2 pdfViewPager;
    private TextView txtTieuDe;
    private View progressContainer;
    private android.widget.ProgressBar readingProgressBar;
    private final SettingsManager settingsManager;
    private PdfPageAdapter pdfPageAdapter;
    private File pdfFile;
    private final StringSupplier titleSupplier;
    private final StringConsumer urlConsumer;
    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    private Runnable onPdfLoaded;
    private Runnable hideProgressRunnable;

    public interface StringSupplier {
        String get();
    }

    public interface StringConsumer {
        void set(String value);
    }

    public PdfViewerController(Context context, ViewPager2 viewPager, TextView tieuDe,
            SettingsManager settingsManager, View progressContainer,
            StringSupplier titleSupplier, StringConsumer urlConsumer) {
        this.context = context;
        this.pdfViewPager = viewPager;
        this.txtTieuDe = tieuDe;
        this.settingsManager = settingsManager;
        this.progressContainer = progressContainer;
        this.readingProgressBar = progressContainer.findViewById(R.id.readingProgressBar);
        this.titleSupplier = titleSupplier;
        this.urlConsumer = urlConsumer;

        // Initialize hide runnable
        hideProgressRunnable = () -> {
            progressContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> progressContainer.setVisibility(View.GONE))
                    .start();
        };
    }

    public void setOnPdfLoaded(Runnable callback) {
        this.onPdfLoaded = callback;
    }

    public void setupPdfRenderer(File pdfFile) {
        this.pdfFile = pdfFile;

        if (pdfPageAdapter != null) {
            pdfViewPager.setAdapter(pdfPageAdapter);
            applySettingsToReader();
            if (onPdfLoaded != null)
                onPdfLoaded.run();
            return;
        }
        try {
            pdfPageAdapter = new PdfPageAdapter(context, pdfFile);
            int savedPageMode = settingsManager.getPageMode();
            if (savedPageMode == 0) {
                settingsManager.setPageMode(isTablet(context) ? 2 : 1);
            }
            pdfPageAdapter.setPageMode(settingsManager.getPageMode());
            pdfViewPager.setAdapter(pdfPageAdapter);
            applySettingsToReader();

            txtTieuDe.setText(titleSupplier.get() + " (" + pdfPageAdapter.getItemCount() + " " +
                    context.getString(R.string.page) + ")");
            Toast.makeText(context, context.getString(R.string.toast_start_reading), Toast.LENGTH_SHORT).show();
            updatePageIndicator(pdfViewPager.getCurrentItem(), pdfPageAdapter.getItemCount());
            if (settingsManager.isAutoNext())
                startAutoNext();

            // Gọi callback khi PDF load xong
            if (onPdfLoaded != null)
                onPdfLoaded.run();

        } catch (Exception e) {
            Log.e("PdfController", "Lỗi setup PdfRenderer", e);
            Toast.makeText(context, context.getString(R.string.toast_cannot_open_pdf), Toast.LENGTH_LONG).show();
        }
    }

    public void setViews(ViewPager2 viewPager, TextView tieuDe, View progressContainer) {
        this.pdfViewPager = viewPager;
        this.txtTieuDe = tieuDe;
        this.progressContainer = progressContainer;
        this.readingProgressBar = progressContainer.findViewById(R.id.readingProgressBar);

        if (pdfPageAdapter != null) {
            setupPdfRenderer(this.pdfFile);
        }
    }

    private boolean isTablet(Context ctx) {
        return ctx.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    public int getCurrentPage() {
        if (pdfViewPager != null && pdfPageAdapter != null)
            return pdfViewPager.getCurrentItem();
        return 0;
    }

    private void updatePageIndicator(int currentPosition, int totalCount) {
        if (progressContainer == null || totalCount == 0)
            return;

        int progress = (int) ((currentPosition + 1) * 100.0 / totalCount);

        readingProgressBar.setProgress(progress);

        // Show with fade animation
        if (progressContainer.getVisibility() == View.GONE) {
            progressContainer.setVisibility(View.VISIBLE);
            progressContainer.setAlpha(0f);
            progressContainer.animate().alpha(1f).setDuration(200).start();
        }

        // Auto-hide after 2 seconds
        progressContainer.removeCallbacks(hideProgressRunnable);
        progressContainer.postDelayed(hideProgressRunnable, 2000);
    }

    public void applySettingsToReader() {
        if (pdfPageAdapter == null || pdfViewPager == null)
            return;
        final int currentPageIndex = pdfViewPager.getCurrentItem();
        final int oldPageMode = pdfPageAdapter.pageMode;

        pdfViewPager.setOrientation(settingsManager.getDirection() == 0 ? ViewPager2.ORIENTATION_HORIZONTAL
                : ViewPager2.ORIENTATION_VERTICAL);
        pdfPageAdapter.setPageMode(settingsManager.getPageMode());
        pdfPageAdapter.notifyDataSetChanged();

        pdfViewPager.post(() -> {
            int finalPos = currentPageIndex;
            if (oldPageMode != settingsManager.getPageMode()) {
                int currentPdfPage = (oldPageMode == 1) ? currentPageIndex : currentPageIndex * 2;
                finalPos = (settingsManager.getPageMode() == 1) ? currentPdfPage : currentPdfPage / 2;
            }
            finalPos = Math.max(0, Math.min(finalPos, pdfPageAdapter.getItemCount() - 1));
            pdfViewPager.setCurrentItem(finalPos, false);
            updatePageIndicator(finalPos, pdfPageAdapter.getItemCount());
        });
    }

    public void setupSettingsView(View settingsContainer, AppCompatButton btnCloseSettings, View btnSettings) {

        TextView txtDirectionTitle = settingsContainer.findViewById(R.id.Chieudoc);
        txtDirectionTitle.setText(context.getString(R.string.Chieudoc));

        RadioGroup rgDirection = settingsContainer.findViewById(R.id.rgReadingDirection);
        RadioButton rbVertical = settingsContainer.findViewById(R.id.rbVertical);
        RadioButton rbHorizontal = settingsContainer.findViewById(R.id.rbHorizontal);
        rbVertical.setText(context.getString(R.string.rbVertical));
        rbHorizontal.setText(context.getString(R.string.rbHorizontal));
        if (settingsManager.getDirection() == 0)
            rgDirection.check(R.id.rbVertical);
        else
            rgDirection.check(R.id.rbHorizontal);

        TextView txtPageModeTitle = settingsContainer.findViewById(R.id.txtPageMode);
        txtPageModeTitle.setText(context.getString(R.string.txtPageMode));

        RadioGroup rgPageMode = settingsContainer.findViewById(R.id.rgPageMode);
        RadioButton rbSinglePage = settingsContainer.findViewById(R.id.rbSinglePage);
        RadioButton rbDoublePage = settingsContainer.findViewById(R.id.rbDoublePage);
        rbSinglePage.setText(context.getString(R.string.rbSinglePage));
        rbDoublePage.setText(context.getString(R.string.rbDoublePage));
        if (settingsManager.getPageMode() == 1)
            rgPageMode.check(R.id.rbSinglePage);
        else
            rgPageMode.check(R.id.rbDoublePage);

        Switch switchAutoNext = settingsContainer.findViewById(R.id.switchAutoNext);
        switchAutoNext.setText(context.getString(R.string.switchAutoNext));
        boolean isAutoNextEnabled = settingsManager.isAutoNext();
        switchAutoNext.setChecked(isAutoNextEnabled);

        View layoutAutoTime = settingsContainer.findViewById(R.id.layoutAutoTime);
        SeekBar seekAutoTime = settingsContainer.findViewById(R.id.seekAutoTime);
        TextView txtAutoTime = settingsContainer.findViewById(R.id.txtAutoTime);
        TextView txtAutoTimeTitle = settingsContainer.findViewById(R.id.txtAutoTimeTitle);
        txtAutoTimeTitle.setText(context.getString(R.string.txtAutoTimeTitle));
        seekAutoTime.setProgress(settingsManager.getAutoTime());
        txtAutoTime.setText(settingsManager.getAutoTime() + "s");
        if (layoutAutoTime != null) {
            layoutAutoTime.setVisibility(isAutoNextEnabled ? View.VISIBLE : View.GONE);
        }

        rgDirection.setOnCheckedChangeListener((group, checkedId) -> {
            settingsManager.setDirection((checkedId == R.id.rbVertical) ? 0 : 1);
            applySettingsToReader();
        });
        rgPageMode.setOnCheckedChangeListener((group, checkedId) -> {
            settingsManager.setPageMode((checkedId == R.id.rbSinglePage) ? 1 : 2);
            applySettingsToReader();
        });
        switchAutoNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setAutoNext(isChecked);
            if (layoutAutoTime != null) {
                layoutAutoTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
            if (isChecked)
                startAutoNext();
            else
                stopAutoNext();
        });
        seekAutoTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1)
                    progress = 1;
                txtAutoTime.setText(progress + "s");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int p = seekBar.getProgress();
                if (p < 1)
                    p = 1;
                settingsManager.setAutoTime(p);
                if (settingsManager.isAutoNext()) {
                    stopAutoNext();
                    startAutoNext();
                }
            }
        });

        btnSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.GONE);
        });
        btnCloseSettings.setText(context.getString(R.string.close));
        btnCloseSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.GONE);
            progressContainer.setVisibility(View.VISIBLE);
        });
    }

    public void startAutoNext() {
        stopAutoNext();
        if (pdfViewPager == null)
            return;

        int delaySec = settingsManager.getAutoTime();
        final long delayMs = (delaySec < 1 ? 3 : delaySec) * 1000L;

        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (pdfPageAdapter == null || pdfViewPager == null)
                    return;
                int current = pdfViewPager.getCurrentItem();
                int total = pdfPageAdapter.getItemCount();
                if (current + 1 < total) {
                    pdfViewPager.setCurrentItem(current + 1, true);
                    autoHandler.postDelayed(this, delayMs);
                } else
                    stopAutoNext();
            }
        };
        autoHandler.postDelayed(autoRunnable, delayMs);
    }

    public void stopAutoNext() {
        if (autoRunnable != null) {
            autoHandler.removeCallbacks(autoRunnable);
            autoRunnable = null;
        }
    }

    public ViewPager2.OnPageChangeCallback getPageChangeCallback() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (pdfPageAdapter != null) {
                    updatePageIndicator(position, pdfPageAdapter.getItemCount());
                }
            }
        };
    }

    public void clearView() {
        this.pdfViewPager = null;
        this.txtTieuDe = null;
        this.progressContainer = null;
        this.readingProgressBar = null;
        stopAutoNext();
        Log.d("PdfController", "Đã xóa tham chiếu View.");
    }

    public void closeRenderer() {
        if (pdfPageAdapter != null) {
            pdfPageAdapter.close();
            pdfPageAdapter = null;
            this.pdfFile = null;
            Log.d("PdfController", "Đã đóng PdfRenderer.");
        }
    }
}
