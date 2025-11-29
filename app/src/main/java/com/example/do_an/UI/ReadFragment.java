package com.example.do_an.UI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.R;
import com.example.do_an.application.NoteActivity;
import com.example.do_an.application.SettingsManager; // Ví dụ
import com.example.do_an.application.DownloadManager; // Ví dụ
import com.example.do_an.application.FavoriteHandler; // Ví dụ
import com.example.do_an.application.HistoryManager; // Ví dụ
import com.example.do_an.application.PdfViewerController; // Ví dụ

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import java.io.File;

// CHÚ Ý: Cần đảm bảo các lớp DownloadManager, PdfViewerController, SettingsManager,
// FavoriteHandler, HistoryManager vẫn hoạt động tốt khi nhận Context từ Fragment (getContext() hoặc getActivity()).
public class ReadFragment extends Fragment implements DownloadManager.LoadingListener {

    private static final String TAG = "ReadFragment";
    private TextView txtTieuDe;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite, btnNote;

    // Biến dữ liệu chính
    private String userEmail;
    private String currentStoryId;
    private String currentTitle;
    private String currentAuthor;
    private String currentCategory;
    private String currentImageUrl;
    private String currentDescription;
    private String mainStoryTitle;
    private String currentReadUrl = ""; // Link PDF đang đọc

    private Call currentDownloadCall; // Dùng để cancel tác vụ tải xuống

    // Khai báo các Manager/Controller
    private SettingsManager settingsManager;
    private PdfViewerController pdfViewerController;
    private DownloadManager downloadManager;
    private FavoriteHandler favoriteHandler;
    private HistoryManager historyManager;
    private TextView txtPageIndicator;
    private ProgressBar progressBarLoading;
    private LinearLayout loadingLayout;

    // Getter cho các lớp khác (chủ yếu cho PdfViewerController)
    public String getCurrentTitle() { return currentTitle; }
    public String getMainStoryTitle() { return mainStoryTitle; }
    private ProgressBar progressDownload;

    // Phương thức tĩnh để tạo instance của Fragment và truyền Bundle (thay cho Intent)
    public static ReadFragment newInstance(Bundle storyData) {
        ReadFragment fragment = new ReadFragment();
        fragment.setArguments(storyData);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Áp dụng Dark Mode (nên làm trong Activity chứa Fragment, nhưng vẫn có thể làm ở đây)
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("darkMode", false);
        // Lưu ý: Việc thay đổi theme toàn cục nên đặt ở Activity
        AppCompatDelegate.setDefaultNightMode(darkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            // Hiển thị Toast và thoát Fragment (hoặc Activity)
            Toast.makeText(context, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            // Yêu cầu Activity chứa nó thoát (hoặc dùng FragmentManager để loại bỏ Fragment)
            if (getActivity() != null) getActivity().finish();
        }

        setupIntentData(getArguments());
        if (currentStoryId == null) {
            Toast.makeText(context, "Lỗi: Không có thông tin truyện!", Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().finish();
        }
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Gán layout Fragment
        return inflater.inflate(R.layout.activity_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo Views
        txtTieuDe = view.findViewById(R.id.txtTieuDe);
        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        progressDownload = view.findViewById(R.id.progressDownload);
        btnNote = view.findViewById(R.id.btnNote);

        // Thiết lập tiêu đề
        if (currentTitle != null) {
            txtTieuDe.setText(currentTitle);
        }

        // Khởi tạo Managers
        Context context = requireContext();
        settingsManager = new SettingsManager(context);
        historyManager = new HistoryManager(context);
        downloadManager = new DownloadManager(context);
        favoriteHandler = new FavoriteHandler(context);

        downloadManager.setLoadingListener(this);

        // Khởi tạo Controller
        pdfViewerController = new PdfViewerController(
                context, pdfViewPager, txtTieuDe, settingsManager,
                txtPageIndicator,
                this::getCurrentTitle,
                (url) -> currentReadUrl = url
        );

        setupViewsAndListeners(view);

        loadPdfContent();

        if (userEmail != null && currentStoryId != null) {
            historyManager.saveStartReadingHistory(
                    userEmail, currentStoryId, mainStoryTitle, currentTitle, currentAuthor
            );
        }
    }

    @Override
    public void showLoading() {
        if (loadingLayout != null) {
            if (isAdded()) { // Đảm bảo Fragment đã được đính kèm vào Activity
                requireActivity().runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.VISIBLE);
                });
            }
        }
    }

    @Override
    public void hideLoading() {
        if (loadingLayout != null) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                });
            }
        }
    }

    // === PHƯƠNG THỨC MỚI CẦN THÊM VÀO ĐỂ KHẮC PHỤC LỖI THIẾU ===
    @Override
    public void hideDownloadProgress() {
        if (progressDownload != null) {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    progressDownload.setVisibility(View.GONE);
                });
            }
        }
    }
    // ==========================================================

    public boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    // Nhận Bundle thay vì Intent
    private void setupIntentData(Bundle args) {
        if (args == null) return;

        String episodeTitle = args.getString("TAP");
        String pdfPath = args.getString("PDF_PATH");

        currentStoryId = args.getString("STORY_ID");
        mainStoryTitle = args.getString("STORY_TITLE");

        currentAuthor = args.getString("STORY_AUTHOR");
        currentCategory = args.getString("STORY_CATEGORY");
        currentImageUrl = args.getString("STORY_IMAGE_URL");
        currentDescription = args.getString("STORY_DESCRIPTION");

        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            currentTitle = episodeTitle;
        } else if (pdfPath != null) {
            currentTitle = new File(pdfPath).getName().replace(".pdf", "");
        } else {
            currentTitle = mainStoryTitle;
        }

        if (mainStoryTitle == null) mainStoryTitle = currentTitle;
    }

    // Dùng View root để tìm kiếm View
    private void setupViewsAndListeners(View root) {
        // Back Button
        root.findViewById(R.id.btnBack).setOnClickListener(v -> {
            // Trong Fragment, nút Back thường dùng để pop back stack hoặc kết thúc Activity
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Favorite Button
        favoriteHandler.checkIfFavorite(currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite);
        btnFavorite.setOnClickListener(v -> favoriteHandler.toggleFavorite(
                userEmail, currentStoryId, mainStoryTitle, currentTitle,
                currentAuthor, currentCategory, currentImageUrl, currentReadUrl,
                btnFavorite
        ));

        // Download Button
        ImageView btnDownLoad = root.findViewById(R.id.btnDown);
        btnDownLoad.setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(getContext(), "Không tìm thấy link PDF!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (progressDownload != null) progressDownload.setVisibility(View.VISIBLE);
            downloadManager.downloadPdfWithOkHttp(currentReadUrl, currentTitle + ".pdf");
        });

        // Nút mở ghi chú
        btnNote.setOnClickListener(v -> {
            int currentPage = pdfViewerController.getCurrentPage() + 1;

            String noteContextId = currentStoryId + "_" + currentTitle;

            Intent intent = new Intent(getContext(), NoteActivity.class);

            intent.putExtra("NOTE_CONTEXT_ID", noteContextId);
            intent.putExtra("PAGE_NUMBER", currentPage);

            intent.putExtra("STORY_TITLE_DISPLAY", currentTitle);

            startActivity(intent);
        });

        // Settings
        pdfViewerController.setupSettingsView(
                root.findViewById(R.id.settingsContainer),
                root.findViewById(R.id.btnCloseSettings),
                root.findViewById(R.id.btnSettings)
        );

        // Page change callback
        if (pdfViewPager != null) {
            pdfViewPager.registerOnPageChangeCallback(pdfViewerController.getPageChangeCallback());
        }
    }

    private void loadPdfContent() {
        Bundle args = getArguments();
        if (args == null) return;

        String episodePdfLink = args.getString("PDF_LINK");
        String pdfPath = args.getString("PDF_PATH");

        currentDownloadCall = downloadManager.loadAndSetupPdf(
                episodePdfLink, pdfPath, mainStoryTitle, pdfViewerController::setupPdfRenderer,
                (url) -> currentReadUrl = url
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        if (userEmail != null) {
            historyManager.saveEndReadingHistory(userEmail);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hủy các tác vụ và giải phóng tài nguyên
        if (currentDownloadCall != null && !currentDownloadCall.isCanceled()) {
            currentDownloadCall.cancel();
        }
        if (pdfViewerController != null) {
            pdfViewerController.closeRenderer();
            pdfViewerController.stopAutoNext();
        }
        if (downloadManager != null) {
            downloadManager.setIsActivityDestroyed(true);
        }
    }
}