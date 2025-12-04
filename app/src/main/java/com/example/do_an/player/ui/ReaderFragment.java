package com.example.do_an.player.ui;

import android.annotation.SuppressLint;
import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.R;
import com.example.do_an.library.downloads.DownloadManager;
import com.example.do_an.library.favorites.FavoriteHandler;
import com.example.do_an.library.history.HistoryManager;
import com.example.do_an.player.pdf.PdfViewerController;

import com.example.do_an.library.notes.NoteFragment;
import com.example.do_an.player.settings.ReaderSettingsManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import java.io.File;

public class ReadFragment extends Fragment implements DownloadManager.LoadingListener {

    private static final String TAG = "ReadFragment";
    private TextView txtTieuDe;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite, btnNote;
    private String userEmail;
    private String currentStoryId;
    private String currentTitle;
    private String currentAuthor;
    private String currentCategory;
    private String currentImageUrl;
    private String currentDescription;
    private String mainStoryTitle;
    private String currentReadUrl = "";
    private Call currentDownloadCall;
    private ReaderSettingsManager readerSettingsManager;
    private PdfViewerController pdfViewerController;
    private DownloadManager downloadManager;
    private FavoriteHandler favoriteHandler;
    private HistoryManager historyManager;
    private TextView txtPageIndicator;
    private ProgressBar progressBarLoading;
    private LinearLayout loadingLayout;
    public String getCurrentTitle() { return currentTitle; }
    public String getMainStoryTitle() { return mainStoryTitle; }
    private ProgressBar progressDownload;
    public static ReadFragment newInstance(Bundle storyData) {
        ReadFragment fragment = new ReadFragment();
        fragment.setArguments(storyData);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("darkMode", false);
        AppCompatDelegate.setDefaultNightMode(darkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
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
        return inflater.inflate(R.layout.ui_activity_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTieuDe = view.findViewById(R.id.txtTieuDe);
        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        progressDownload = view.findViewById(R.id.progressDownload);
        btnNote = view.findViewById(R.id.btnNote);

        if (currentTitle != null) {
            txtTieuDe.setText(currentTitle);
        }

        Context context = requireContext();
        readerSettingsManager = new ReaderSettingsManager(context);
        historyManager = new HistoryManager(context);
        downloadManager = new DownloadManager(context);
        favoriteHandler = new FavoriteHandler(context);

        downloadManager.setLoadingListener(this);
        downloadManager.setTxtPageIndicator(txtPageIndicator);

        pdfViewerController = new PdfViewerController(
                context, pdfViewPager, txtTieuDe, readerSettingsManager,
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

    public boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

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

    private void handleBackPressed() {
        // Thử pop back stack từ parent fragment manager trước
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
            return;
        }
        
        // Nếu không có back stack entry, thử với Activity's fragment manager
        if (getActivity() != null) {
            if (getActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getActivity().getSupportFragmentManager().popBackStack();
            } else {
                // Sử dụng OnBackPressedDispatcher thay vì deprecated onBackPressed()
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        }
    }

    private void setupViewsAndListeners(View root) {
        // Back Button
        root.findViewById(R.id.btnBack).setOnClickListener(v -> handleBackPressed());

        favoriteHandler.checkIfFavorite(currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite);
        btnFavorite.setOnClickListener(v -> favoriteHandler.toggleFavorite(
                userEmail, currentStoryId, mainStoryTitle, currentTitle,
                currentAuthor, currentCategory, currentImageUrl, currentReadUrl,
                btnFavorite
        ));

        ImageView btnDownLoad = root.findViewById(R.id.btnDown);
        btnDownLoad.setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(getContext(), "Không tìm thấy link PDF!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (progressDownload != null) progressDownload.setVisibility(View.VISIBLE);
            downloadManager.downloadPdfWithOkHttp(currentReadUrl, currentTitle + ".pdf");
        });

        btnNote.setOnClickListener(v -> {
            if (getParentFragmentManager() == null) {
                Toast.makeText(getContext(), "Lỗi: Không tìm thấy Fragment Manager.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentPage = pdfViewerController.getCurrentPage() + 1;

            String displayTitle = currentTitle != null ? currentTitle : mainStoryTitle;
            if (displayTitle == null) displayTitle = "Truyện";

            String noteContextId = currentStoryId + "_" + displayTitle;

            NoteFragment noteFragment = NoteFragment.newInstance(
                    noteContextId,
                    currentPage,
                    displayTitle
            );

            getParentFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, noteFragment)
                    .addToBackStack(null)
                    .commit();
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
        if (currentDownloadCall != null && !currentDownloadCall.isCanceled()) {
            currentDownloadCall.cancel();
        }
        if (pdfViewerController != null) {
            pdfViewerController.stopAutoNext();

            pdfViewerController.closeRenderer();
        }
        if (downloadManager != null) {
            downloadManager.setIsActivityDestroyed(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pdfViewerController != null) {
            pdfViewerController.closeRenderer();
        }
    }
}