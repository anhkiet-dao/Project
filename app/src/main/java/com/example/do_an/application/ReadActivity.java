package com.example.do_an.application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.do_an.R;
import com.example.do_an.Story.FavoriteManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import java.io.File;

public class ReadActivity extends AppCompatActivity implements DownloadManager.LoadingListener {

    private static final String TAG = "ReadActivity";
    private TextView txtTieuDe;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Áp dụng Dark Mode
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(darkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        txtTieuDe = findViewById(R.id.txtTieuDe);
        pdfViewPager = findViewById(R.id.pdfViewPager);
        btnFavorite = findViewById(R.id.btnFavorite);
        txtPageIndicator = findViewById(R.id.txtPageIndicator);
        loadingLayout = findViewById(R.id.loadingLayout);
        progressDownload = findViewById(R.id.progressDownload);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupIntentData(getIntent());
        if (currentStoryId == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin truyện!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        txtTieuDe.setText(currentTitle);

        settingsManager = new SettingsManager(this);
        historyManager = new HistoryManager(this);
        downloadManager = new DownloadManager(this);
        favoriteHandler = new FavoriteHandler(this);

        downloadManager.setLoadingListener(this);

        pdfViewerController = new PdfViewerController(
                this, pdfViewPager, txtTieuDe, settingsManager,
                txtPageIndicator,
                this::getCurrentTitle,
                (url) -> currentReadUrl = url
        );

        setupViewsAndListeners();

        loadPdfContent();

        historyManager.saveStartReadingHistory(
                userEmail, currentStoryId, mainStoryTitle, currentTitle, currentAuthor
        );
    }

    @Override
    public void showLoading() {
        runOnUiThread(() -> {
            loadingLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void hideLoading() {
        runOnUiThread(() -> {
            loadingLayout.setVisibility(View.GONE);
        });
    }

    public boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private void setupIntentData(Intent intent) {
        String episodeTitle = intent.getStringExtra("TAP");
        String pdfPath = intent.getStringExtra("PDF_PATH");

        currentStoryId = intent.getStringExtra("STORY_ID");
        mainStoryTitle = intent.getStringExtra("STORY_TITLE"); // Tên truyện chính

        currentAuthor = intent.getStringExtra("STORY_AUTHOR");
        currentCategory = intent.getStringExtra("STORY_CATEGORY");
        currentImageUrl = intent.getStringExtra("STORY_IMAGE_URL");
        currentDescription = intent.getStringExtra("STORY_DESCRIPTION");

        // Xác định tên hiển thị trên màn hình
        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            currentTitle = episodeTitle; // Ưu tiên tên tập "Tập 01"
        } else if (pdfPath != null) {
            // Lấy tên file local (cần logic xử lý nếu có)
            currentTitle = new File(pdfPath).getName().replace(".pdf", "");
        } else {
            currentTitle = mainStoryTitle; // Tên truyện chính
        }

        // Đảm bảo mainStoryTitle không bị null (dùng cho DownloadManager)
        if (mainStoryTitle == null) mainStoryTitle = currentTitle;
    }

    private void setupViewsAndListeners() {
        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Favorite Button
        favoriteHandler.checkIfFavorite(currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite);
        btnFavorite.setOnClickListener(v -> favoriteHandler.toggleFavorite(
                userEmail, currentStoryId, mainStoryTitle, currentTitle,
                currentAuthor, currentCategory, currentImageUrl, currentReadUrl,
                btnFavorite
        ));

        // Download Button
        ImageView btnDownLoad = findViewById(R.id.btnDown);
        btnDownLoad.setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy link PDF!", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDownload.setVisibility(View.VISIBLE);
            // Gọi DownloadManager
            downloadManager.downloadPdfWithOkHttp(currentReadUrl, currentTitle + ".pdf");
        });

        // Settings (Gọi Controller để xử lý View Settings)
        pdfViewerController.setupSettingsView(
                findViewById(R.id.settingsContainer),
                findViewById(R.id.btnCloseSettings),
                findViewById(R.id.btnSettings)
        );

        // Page change callback (gọi Controller)
        pdfViewPager.registerOnPageChangeCallback(pdfViewerController.getPageChangeCallback());
    }

    private void loadPdfContent() {
        String episodePdfLink = getIntent().getStringExtra("PDF_LINK");
        String pdfPath = getIntent().getStringExtra("PDF_PATH");

        // Gọi DownloadManager để xác định nguồn tải và xử lý
        currentDownloadCall = downloadManager.loadAndSetupPdf(
                episodePdfLink, pdfPath, mainStoryTitle, pdfViewerController::setupPdfRenderer,
                (url) -> currentReadUrl = url // Cập nhật URL tìm được từ Firestore
        );
    }

    // --- Vòng đời Activity ---
    @Override
    protected void onPause() {
        super.onPause();
        historyManager.saveEndReadingHistory(userEmail); // Lưu key đã có từ start
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentDownloadCall != null && !currentDownloadCall.isCanceled()) {
            currentDownloadCall.cancel();
        }
        pdfViewerController.closeRenderer();
        pdfViewerController.stopAutoNext();
        downloadManager.setIsActivityDestroyed(true);
    }
}