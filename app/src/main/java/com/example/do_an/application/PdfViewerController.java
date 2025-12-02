package com.example.do_an.application;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.R;

import java.io.File;

public class PdfViewerController {

    private final Context context;
    // <<< THAY ĐỔI: Không dùng final cho ViewPager2 và TextView nữa
    private ViewPager2 pdfViewPager;
    private TextView txtTieuDe;
    private TextView txtPageIndicator;
    // >>>

    private final SettingsManager settingsManager;
    private PdfPageAdapter pdfPageAdapter;
    private File pdfFile; // <<< THÊM: Lưu trữ File PDF

    private final StringSupplier titleSupplier;
    private final StringConsumer urlConsumer;

    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    public interface StringSupplier { String get(); }
    public interface StringConsumer { void set(String value); }
    public PdfViewerController(Context context, ViewPager2 viewPager, TextView tieuDe,
                               SettingsManager settingsManager, TextView pageIndicator,
                               StringSupplier titleSupplier, StringConsumer urlConsumer) {
        this.context = context;
        this.pdfViewPager = viewPager; // Lần khởi tạo đầu tiên
        this.txtTieuDe = tieuDe;
        this.settingsManager = settingsManager;
        this.txtPageIndicator = pageIndicator;
        this.titleSupplier = titleSupplier;
        this.urlConsumer = urlConsumer;
    }
    public void setupPdfRenderer(File pdfFile) {
        this.pdfFile = pdfFile; // <<< LƯU FILE PDF

        if (pdfPageAdapter != null) {
            // Trường hợp 1: Fragment View bị hủy và tạo lại (tái sử dụng Adapter và Renderer)

            // Cần gán lại Adapter cho ViewPager2 mới
            pdfViewPager.setAdapter(pdfPageAdapter);

            applySettingsToReader(); // Áp dụng lại cài đặt và vị trí trang

            Log.d(TAG, "Renderer đã tồn tại, tái sử dụng Adapter.");
            return;
        }
        try {
            pdfPageAdapter = new PdfPageAdapter(context, pdfFile);

            final int DEFAULT_PAGE_MODE = 0;
            final int SINGLE_PAGE_MODE = 1;
            final int DOUBLE_PAGE_MODE = 2;

            int savedPageMode = settingsManager.getPageMode();

            if (savedPageMode == DEFAULT_PAGE_MODE) {
                if (isTablet(context)) {
                    settingsManager.setPageMode(DOUBLE_PAGE_MODE);
                } else {
                    settingsManager.setPageMode(SINGLE_PAGE_MODE);
                }
            }

            pdfPageAdapter.setPageMode(settingsManager.getPageMode());
            pdfViewPager.setAdapter(pdfPageAdapter);

            applySettingsToReader();

            txtTieuDe.setText(titleSupplier.get() + " (" + pdfPageAdapter.getItemCount() + " trang)");
            Toast.makeText(context, "Tải xong, bắt đầu đọc!", Toast.LENGTH_SHORT).show();

            updatePageIndicator(pdfViewPager.getCurrentItem(), pdfPageAdapter.getItemCount());

            if (settingsManager.isAutoNext()) startAutoNext();

        } catch (Exception e) {
            Log.e("PdfController", "Lỗi setup PdfRenderer", e);
            Toast.makeText(context, "Không thể mở file PDF đã tải.", Toast.LENGTH_LONG).show();
        }
    }

    public void setViews(ViewPager2 viewPager, TextView tieuDe, TextView pageIndicator) {
        this.pdfViewPager = viewPager;
        this.txtTieuDe = tieuDe;
        this.txtPageIndicator = pageIndicator;

        if (pdfPageAdapter != null) {
            setupPdfRenderer(this.pdfFile);
        }
    }

    private boolean isTablet(Context ctx) {
        return ctx.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    public int getCurrentPage() {
        // Kiểm tra null cho ViewPager2 (vì nó có thể đã bị clear)
        if (pdfViewPager != null && pdfPageAdapter != null) {
            return pdfViewPager.getCurrentItem();
        }
        return 0;
    }

    private void updatePageIndicator(int currentPosition, int totalCount) {
        if (txtPageIndicator != null) {
            txtPageIndicator.setText((currentPosition + 1) + "/" + totalCount);
        }
    }

    public void applySettingsToReader() {
        if (pdfPageAdapter == null || pdfViewPager == null) return;

        final int currentPageIndex = pdfViewPager.getCurrentItem();
        final int oldPageMode = pdfPageAdapter.pageMode;

        int newDir = settingsManager.getDirection();
        pdfViewPager.setOrientation(newDir == 0 ?
                ViewPager2.ORIENTATION_VERTICAL : ViewPager2.ORIENTATION_HORIZONTAL);

        final int newPageMode = settingsManager.getPageMode();
        pdfPageAdapter.setPageMode(newPageMode);

        pdfPageAdapter.notifyDataSetChanged();

        pdfViewPager.post(() -> {

            int finalPosition = currentPageIndex;

            if (oldPageMode != newPageMode) {
                int currentPdfPage = (oldPageMode == 1) ? currentPageIndex : currentPageIndex * 2;

                finalPosition = (newPageMode == 1) ? currentPdfPage : currentPdfPage / 2;
            }

            int maxPosition = pdfPageAdapter.getItemCount() - 1;
            if (finalPosition > maxPosition) finalPosition = maxPosition;
            if (finalPosition < 0) finalPosition = 0;

            pdfViewPager.setCurrentItem(finalPosition, false);

            updatePageIndicator(finalPosition, pdfPageAdapter.getItemCount());
        });
    }

    public void setupSettingsView(View settingsContainer, AppCompatButton btnCloseSettings, View btnSettings) {
        RadioGroup rgDirection = settingsContainer.findViewById(R.id.rgReadingDirection);
        RadioGroup rgPageMode = settingsContainer.findViewById(R.id.rgPageMode);
        Switch switchAutoNext = settingsContainer.findViewById(R.id.switchAutoNext);

        View layoutAutoTime = settingsContainer.findViewById(R.id.layoutAutoTime);
        SeekBar seekAutoTime = settingsContainer.findViewById(R.id.seekAutoTime);
        TextView txtAutoTime = settingsContainer.findViewById(R.id.txtAutoTime);

        if (settingsManager.getDirection() == 0) rgDirection.check(R.id.rbVertical);
        else rgDirection.check(R.id.rbHorizontal);
        if (settingsManager.getPageMode() == 1) rgPageMode.check(R.id.rbSinglePage);
        else rgPageMode.check(R.id.rbDoublePage);

        boolean isAutoNextEnabled = settingsManager.isAutoNext();
        switchAutoNext.setChecked(isAutoNextEnabled);
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

            if (isChecked) startAutoNext();
            else stopAutoNext();
        });

        seekAutoTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) progress = 1; // Đảm bảo thời gian tối thiểu là 1s
                txtAutoTime.setText(progress + "s");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                int p = seekBar.getProgress();
                if (p < 1) p = 1;
                settingsManager.setAutoTime(p); // Lưu giá trị mới

                if (settingsManager.isAutoNext()) { stopAutoNext(); startAutoNext(); }
            }
        });

        btnSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.VISIBLE);
            txtPageIndicator.setVisibility(View.GONE); // ẨN khi mở Cài đặt
        });

        btnCloseSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.GONE);
            txtPageIndicator.setVisibility(View.VISIBLE); // HIỆN lại khi đóng
        });
    }

    public void startAutoNext() {
        stopAutoNext();
        if (pdfViewPager == null) return; // Thêm kiểm tra

        int delaySec = settingsManager.getAutoTime();
        final long delayMs = (delaySec < 1 ? 3 : delaySec) * 1000L;

        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (pdfPageAdapter == null || pdfViewPager == null) return; // Thêm kiểm tra
                int current = pdfViewPager.getCurrentItem();
                int total = pdfPageAdapter.getItemCount();

                if (current + 1 < total) {
                    pdfViewPager.setCurrentItem(current + 1, true);
                    autoHandler.postDelayed(this, delayMs);
                } else {
                    stopAutoNext();
                }
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
                // Bạn có thể lưu vị trí đọc hiện tại vào SharedPreferences ở đây nếu cần
                if (pdfPageAdapter != null) {
                    updatePageIndicator(position, pdfPageAdapter.getItemCount());
                }
            }
        };
    }

    public void clearView() {
        // Xóa tham chiếu tới View để tránh rò rỉ bộ nhớ
        this.pdfViewPager = null;
        this.txtTieuDe = null;
        this.txtPageIndicator = null;
        stopAutoNext(); // Đảm bảo dừng tác vụ tự động chuyển trang
        Log.d("PdfController", "Đã xóa tham chiếu View.");
    }
    public void closeRenderer() {
        if (pdfPageAdapter != null) {
            pdfPageAdapter.close();
            pdfPageAdapter = null; // Thiết lập Adapter về null
            this.pdfFile = null; // Xóa tham chiếu File
            Log.d("PdfController", "Đã đóng PdfRenderer.");
        }
    }
}