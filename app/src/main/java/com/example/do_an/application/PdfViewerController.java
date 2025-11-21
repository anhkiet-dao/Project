package com.example.do_an.application;

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
    private final ReadActivity activity;
    private final ViewPager2 pdfViewPager;
    private final TextView txtTieuDe;
    private final SettingsManager settingsManager;
    private PdfPageAdapter pdfPageAdapter;
    private final TextView txtPageIndicator;

    private final StringSupplier titleSupplier;
    private final StringConsumer urlConsumer;

    private Handler autoHandler = new Handler();
    private Runnable autoRunnable;
    public interface StringSupplier { String get(); }
    public interface StringConsumer { void set(String value); }

    public PdfViewerController(ReadActivity activity, ViewPager2 viewPager, TextView tieuDe,
                               SettingsManager settingsManager, TextView pageIndicator, // <<< TextView pageIndicator là tham số thứ 5
                               StringSupplier titleSupplier, StringConsumer urlConsumer) { // <<< StringSupplier là thứ 6
        this.activity = activity;
        this.pdfViewPager = viewPager;
        this.txtTieuDe = tieuDe;
        this.settingsManager = settingsManager;
        this.txtPageIndicator = pageIndicator;
        this.titleSupplier = titleSupplier;
        this.urlConsumer = urlConsumer;
    }

    // --- Core Logic: Setup và Apply Settings ---
    public void setupPdfRenderer(File pdfFile) {
        try {
            pdfPageAdapter = new PdfPageAdapter(activity, pdfFile);

            // --- Bổ sung Logic Tự động chọn PageMode ---

            // Giả sử: 0 là giá trị mặc định ban đầu (chưa được cài đặt)
            final int DEFAULT_PAGE_MODE = 0;
            final int SINGLE_PAGE_MODE = 1;
            final int DOUBLE_PAGE_MODE = 2;

            int savedPageMode = settingsManager.getPageMode();

            if (savedPageMode == DEFAULT_PAGE_MODE) {
                if (activity.isTablet()) {
                    settingsManager.setPageMode(DOUBLE_PAGE_MODE);
                } else {
                    settingsManager.setPageMode(SINGLE_PAGE_MODE);
                }
            }

            // --- Kết thúc Logic Tự động chọn PageMode ---

            pdfPageAdapter.setPageMode(settingsManager.getPageMode());
            pdfViewPager.setAdapter(pdfPageAdapter);

            applySettingsToReader(); // Áp dụng direction, page mode, và set Item

            txtTieuDe.setText(titleSupplier.get() + " (" + pdfPageAdapter.getItemCount() + " trang)");
            Toast.makeText(activity, "Tải xong, bắt đầu đọc!", Toast.LENGTH_SHORT).show();

            updatePageIndicator(pdfViewPager.getCurrentItem(), pdfPageAdapter.getItemCount());

            if (settingsManager.isAutoNext()) startAutoNext();

        } catch (Exception e) {
            Log.e("PdfController", "Lỗi setup PdfRenderer", e);
            Toast.makeText(activity, "Không thể mở file PDF đã tải.", Toast.LENGTH_LONG).show();
        }
    }

    public int getCurrentPage() {
        if (pdfViewPager != null && pdfPageAdapter != null) {
            // ViewPager2.getCurrentItem() trả về vị trí index (0-based)
            return pdfViewPager.getCurrentItem();
        }
        return 0;
    }

    private void updatePageIndicator(int currentPosition, int totalCount) {
        if (txtPageIndicator != null) {
            // Vì ViewPager2 là 0-indexed, ta cộng thêm 1 để hiển thị trang 1/N
            txtPageIndicator.setText((currentPosition + 1) + "/" + totalCount);
        }
    }

    // Trong PdfViewerController.java
    public void applySettingsToReader() {
        if (pdfPageAdapter == null) return;

        // 1. LƯU VỊ TRÍ HIỆN TẠI VÀ CHẾ ĐỘ TRANG CŨ
        final int currentPageIndex = pdfViewPager.getCurrentItem();
        final int oldPageMode = pdfPageAdapter.pageMode;

        // 2. Cập nhật orientation
        int newDir = settingsManager.getDirection();
        pdfViewPager.setOrientation(newDir == 0 ?
                ViewPager2.ORIENTATION_VERTICAL : ViewPager2.ORIENTATION_HORIZONTAL);

        // 3. Cập nhật pageMode
        final int newPageMode = settingsManager.getPageMode();
        pdfPageAdapter.setPageMode(newPageMode);

        // 4. BUỘC ADAPTER CẬP NHẬT LẠI DỮ LIỆU
        pdfPageAdapter.notifyDataSetChanged();

        // 5. TRÌ HOÃN (POST) việc đặt lại trang
        pdfViewPager.post(() -> {

            // --- LOGIC TÍNH TOÁN VỊ TRÍ MỚI (Dùng lại logic cũ) ---
            int finalPosition = currentPageIndex;

            if (oldPageMode != newPageMode) {
                int currentPdfPage = (oldPageMode == 1) ? currentPageIndex : currentPageIndex * 2;

                finalPosition = (newPageMode == 1) ? currentPdfPage : currentPdfPage / 2;
            }

            // Đảm bảo vị trí không vượt quá giới hạn
            int maxPosition = pdfPageAdapter.getItemCount() - 1;
            if (finalPosition > maxPosition) finalPosition = maxPosition;
            if (finalPosition < 0) finalPosition = 0;

            // 6. Set CurrentItem
            pdfViewPager.setCurrentItem(finalPosition, false);

            // Cập nhật lại chỉ số trang
            updatePageIndicator(finalPosition, pdfPageAdapter.getItemCount());
        });
    }

    // --- Logic: Settings View ---
    public void setupSettingsView(View settingsContainer, AppCompatButton btnCloseSettings, View btnSettings) {
        // 1. Lấy tham chiếu đến các Views

        // Controls chung
        RadioGroup rgDirection = settingsContainer.findViewById(R.id.rgReadingDirection);
        RadioGroup rgPageMode = settingsContainer.findViewById(R.id.rgPageMode);
        Switch switchAutoNext = settingsContainer.findViewById(R.id.switchAutoNext);

        // Controls Tự động chuyển trang
        // Lưu ý: layoutAutoTime là LinearLayout cha chứa SeekBar và TextView thời gian.
        View layoutAutoTime = settingsContainer.findViewById(R.id.layoutAutoTime); // <<< THÊM: Lấy View Layout cha
        SeekBar seekAutoTime = settingsContainer.findViewById(R.id.seekAutoTime);
        TextView txtAutoTime = settingsContainer.findViewById(R.id.txtAutoTime);


        // 2. Load trạng thái ban đầu từ SettingsManager

        // Cài đặt hướng đọc và chế độ trang
        if (settingsManager.getDirection() == 0) rgDirection.check(R.id.rbVertical);
        else rgDirection.check(R.id.rbHorizontal);
        if (settingsManager.getPageMode() == 1) rgPageMode.check(R.id.rbSinglePage);
        else rgPageMode.check(R.id.rbDoublePage);

        // Cài đặt Tự động chuyển trang
        boolean isAutoNextEnabled = settingsManager.isAutoNext();
        switchAutoNext.setChecked(isAutoNextEnabled);
        seekAutoTime.setProgress(settingsManager.getAutoTime());
        txtAutoTime.setText(settingsManager.getAutoTime() + "s");

        // <<< QUAN TRỌNG: Thiết lập visibility ban đầu cho SeekBar
        if (layoutAutoTime != null) {
            layoutAutoTime.setVisibility(isAutoNextEnabled ? View.VISIBLE : View.GONE);
        }


        // 3. Thiết lập Listeners
        rgDirection.setOnCheckedChangeListener((group, checkedId) -> {
            settingsManager.setDirection((checkedId == R.id.rbVertical) ? 0 : 1);
            applySettingsToReader();
        });
        rgPageMode.setOnCheckedChangeListener((group, checkedId) -> {
            settingsManager.setPageMode((checkedId == R.id.rbSinglePage) ? 1 : 2);
            applySettingsToReader();
        });

        // Listener cho Switch Tự động chuyển trang
        switchAutoNext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setAutoNext(isChecked);

            // Cập nhật hiển thị Layout Auto Time
            if (layoutAutoTime != null) {
                layoutAutoTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }

            // Bắt đầu hoặc Dừng chức năng tự động chuyển trang
            if (isChecked) startAutoNext();
            else stopAutoNext();
        });

        // Listener cho SeekBar điều chỉnh thời gian
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

                // Khởi động lại Auto Next nếu đang chạy để áp dụng tốc độ mới
                if (settingsManager.isAutoNext()) { stopAutoNext(); startAutoNext(); }
            }
        });

        // 4. Toggle View (Hiển thị/Ẩn Container Cài đặt)
        btnSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.VISIBLE);
            txtPageIndicator.setVisibility(View.GONE); // ẨN khi mở Cài đặt
        });

        btnCloseSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.GONE);
            txtPageIndicator.setVisibility(View.VISIBLE); // HIỆN lại khi đóng
        });
    }

    // --- Logic: Auto Next ---
    public void startAutoNext() {
        stopAutoNext();
        int delaySec = settingsManager.getAutoTime();
        final long delayMs = (delaySec < 1 ? 3 : delaySec) * 1000L;

        autoRunnable = new Runnable() {
            @Override
            public void run() {
                if (pdfPageAdapter == null) return;
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

    // --- Logic: ViewPager2 Callback ---
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

    // --- Logic: Cleanup ---
    public void closeRenderer() {
        if (pdfPageAdapter != null) {
            pdfPageAdapter.close();
            Log.d("PdfController", "Đã đóng PdfRenderer.");
        }
    }
}