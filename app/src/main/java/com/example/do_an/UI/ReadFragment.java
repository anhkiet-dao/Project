package com.example.do_an.UI;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.Note.NoteFragment;
import com.example.do_an.R;
import com.example.do_an.application.SettingsManager;
import com.example.do_an.Download.DownloadManager;
import com.example.do_an.Favorite.FavoriteHandler;
import com.example.do_an.History.HistoryManager;
import com.example.do_an.pdf.PdfViewerController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import java.io.File;

import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

public class ReadFragment extends Fragment implements DownloadManager.LoadingListener {

    private static final String TAG = "ReadFragment";
    private TextView txtTieuDe, txtPageIndicator, txtLoading;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite, btnNote, btnFullScreenAction;
    private LinearLayout topBar, rootLayout, loadingLayout;
    private ProgressBar progressDownload;

    private String userEmail;
    private String currentStoryId, currentTitle, mainStoryTitle;
    private String currentAuthor, currentCategory, currentImageUrl, currentDescription;
    private String currentReadUrl = "";
    private String episodePdfLink, pdfPath;

    private Call currentDownloadCall;
    private SettingsManager settingsManager;
    private PdfViewerController pdfViewerController;
    private DownloadManager downloadManager;
    private FavoriteHandler favoriteHandler;
    private HistoryManager historyManager;
    private DownloadedPdfDao pdfDao;

    private boolean isFullScreenMode = false;
    private NavigationListener navigationListener;

    public static ReadFragment newInstance(Bundle storyData) {
        ReadFragment fragment = new ReadFragment();
        fragment.setArguments(storyData);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            navigationListener = (NavigationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement NavigationListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = requireContext();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(context, getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            if (getActivity() != null)
                getActivity().finish();
        }

        setupIntentData(getArguments());

        Log.d(TAG, "--- FINAL DATA CHECK ---");
        Log.d(TAG, "currentTitle: " + currentTitle);
        Log.d(TAG, "episodePdfLink (Online URL): " + episodePdfLink);
        Log.d(TAG, "pdfPath (Offline Path): " + pdfPath);
        Log.d(TAG, "------------------------");

        if (currentStoryId == null && pdfPath == null) {
            Toast.makeText(context, getString(R.string.no_story_data), Toast.LENGTH_LONG).show();
            if (getActivity() != null)
                getActivity().finish();
        }
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_activity_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtTieuDe = view.findViewById(R.id.txtTieuDe);
        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);
        loadingLayout = view.findViewById(R.id.loadingLayout);
        progressDownload = view.findViewById(R.id.progressDownload);
        btnNote = view.findViewById(R.id.btnNote);
        btnFullScreenAction = view.findViewById(R.id.btnfull);
        topBar = view.findViewById(R.id.topBar);
        rootLayout = (LinearLayout) view;
        txtLoading = view.findViewById(R.id.txtLoading);

        if (currentTitle != null) txtTieuDe.setText(currentTitle);

        Context context = requireContext();
        settingsManager = new SettingsManager(context);
        historyManager = new HistoryManager(context);
        favoriteHandler = new FavoriteHandler(context);

        AppDatabase db = AppDatabase.getDatabase(context);
        pdfDao = db.downloadedPdfDao();
        downloadManager = new DownloadManager(context, pdfDao);

        downloadManager.setLoadingListener(this);
        downloadManager.setTxtPageIndicator(txtPageIndicator);

        pdfViewerController = new PdfViewerController(
                context, pdfViewPager, txtTieuDe, settingsManager,
                txtPageIndicator,
                this::getCurrentTitle,
                (url) -> currentReadUrl = url
        );

        if (pdfPath != null && !pdfPath.trim().isEmpty()) {
            findAndSetupStoryInfoFromRoom(pdfPath);
        } else {
            checkMandatoryStoryInfo();
            setupViewsAndListeners(view);
            loadPdfContent();

            if (userEmail != null && currentStoryId != null) {
                historyManager.saveStartReadingHistory(
                        userEmail, currentStoryId, mainStoryTitle, currentTitle,
                        currentAuthor, currentImageUrl
                );
            }
        }
    }

    private void findAndSetupStoryInfoFromRoom(String filePath) {
        showLoading();

        new Thread(() -> {
            DownloadedPdfEntity entity = pdfDao.getPdfByFilePath(filePath);

            requireActivity().runOnUiThread(() -> {
                if (entity != null) {
                    currentStoryId = entity.storyDocumentId;
                    String savedFileName = entity.fileName.replace(".pdf", "");
                    currentTitle = savedFileName;
                    mainStoryTitle = savedFileName;

                    currentAuthor = entity.author != null ? entity.author : getString(R.string.author_unknown);
                    currentCategory = getString(R.string.downloaded);
                    currentReadUrl = entity.pdfUrl;

                    txtTieuDe.setText(currentTitle);

                    checkMandatoryStoryInfo();
                    setupViewsAndListeners(getView());
                    loadPdfContent();

                    if (userEmail != null && currentStoryId != null) {
                        historyManager.saveStartReadingHistory(
                                userEmail, currentStoryId, mainStoryTitle, currentTitle,
                                currentAuthor, currentImageUrl
                        );
                    }
                    hideLoading();
                } else {
                    Log.e(TAG, "ERROR: Không tìm thấy Entity trong Room bằng path: " + filePath);
                    Toast.makeText(getContext(), getString(R.string.story_not_found_room), Toast.LENGTH_LONG).show();
                    hideLoading();
                }
            });
        }).start();
    }

    private void checkMandatoryStoryInfo() {
        if (currentStoryId == null) {
            if (mainStoryTitle != null && !mainStoryTitle.isEmpty()) {
                currentStoryId = mainStoryTitle;
            } else if (currentTitle != null && !currentTitle.isEmpty()) {
                currentStoryId = currentTitle;
            } else {
                Log.e(TAG, "Không thể xác định Story ID!");
            }
        }
        if (currentAuthor == null) currentAuthor = getString(R.string.author_unknown);
    }

    private void setupIntentData(Bundle args) {
        if (args == null) return;

        episodePdfLink = args.getString("PDF_LINK");
        pdfPath = args.getString("PDF_PATH");

        currentStoryId = args.getString("STORY_ID");
        mainStoryTitle = args.getString("STORY_TITLE");
        currentAuthor = args.getString("STORY_AUTHOR");
        currentCategory = args.getString("STORY_CATEGORY");
        currentImageUrl = args.getString("STORY_IMAGE_URL");
        currentDescription = args.getString("STORY_DESCRIPTION");

        String episodeTitle = args.getString("TAP_TITLE");
        if (episodeTitle == null || episodeTitle.isEmpty()) episodeTitle = args.getString("TAP");

        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            currentTitle = episodeTitle;
        } else if (pdfPath != null) {
            currentTitle = new File(pdfPath).getName().replace(".pdf", "");
            if (mainStoryTitle == null) mainStoryTitle = currentTitle;
        } else currentTitle = mainStoryTitle;

        if (mainStoryTitle == null) mainStoryTitle = currentTitle;
    }

    private void setupViewsAndListeners(View root) {
        root.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        favoriteHandler.checkIfFavorite(currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite);
        btnFavorite.setOnClickListener(v -> favoriteHandler.toggleFavorite(
                userEmail, currentStoryId, mainStoryTitle, currentTitle,
                currentAuthor, currentCategory, currentImageUrl, currentReadUrl, btnFavorite
        ));

        ImageView btnDownLoad = root.findViewById(R.id.btnDown);
        btnDownLoad.setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.no_pdf_link), Toast.LENGTH_SHORT).show();
                return;
            }
            progressDownload.setVisibility(View.VISIBLE);

            String fullFileName = mainStoryTitle;
            if (currentTitle != null && !currentTitle.isEmpty() && !currentTitle.equals(mainStoryTitle)) {
                fullFileName += " - " + currentTitle;
            }
            fullFileName += ".pdf";

            downloadManager.downloadPdfWithOkHttp(
                    currentReadUrl,
                    fullFileName,
                    currentStoryId,
                    currentAuthor,
                    currentImageUrl
            );
        });

        btnNote.setOnClickListener(v -> {
            if (getParentFragmentManager() == null) {
                Toast.makeText(getContext(), getString(R.string.no_fragment_manager), Toast.LENGTH_SHORT).show();
                return;
            }

            int currentPage = pdfViewerController.getCurrentPage() + 1;
            String displayTitle = currentTitle != null ? currentTitle : mainStoryTitle;
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

        btnFullScreenAction.setOnClickListener(v -> toggleFullScreenMode());
        btnFullScreenAction.setVisibility(View.VISIBLE);
        btnFullScreenAction.setBackgroundResource(android.R.color.transparent);

        pdfViewerController.setupSettingsView(
                root.findViewById(R.id.settingsContainer),
                root.findViewById(R.id.btnCloseSettings),
                root.findViewById(R.id.btnSettings)
        );

        if (pdfViewPager != null) {
            pdfViewPager.registerOnPageChangeCallback(pdfViewerController.getPageChangeCallback());
        }
    }

    private void toggleFullScreenMode() {
        isFullScreenMode = !isFullScreenMode;
        final int originalPaddingBottom = (int) (getResources().getDisplayMetrics().density * 47);

        if (isFullScreenMode) {
            if (topBar != null) topBar.setVisibility(View.GONE);
            if (txtPageIndicator != null) txtPageIndicator.setVisibility(View.VISIBLE);
            if (rootLayout != null) rootLayout.setPadding(rootLayout.getPaddingLeft(),
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight(),
                    0);

            if (navigationListener != null) navigationListener.setBottomNavVisibility(View.GONE);
            if (btnFullScreenAction != null) btnFullScreenAction.setImageResource(R.drawable.ic_exit_full);
        } else {
            if (topBar != null) topBar.setVisibility(View.VISIBLE);
            if (txtPageIndicator != null && settingsManager.isPageIndicatorEnabled())
                txtPageIndicator.setVisibility(View.VISIBLE);
            if (rootLayout != null) rootLayout.setPadding(rootLayout.getPaddingLeft(),
                    rootLayout.getPaddingTop(),
                    rootLayout.getPaddingRight(),
                    originalPaddingBottom);
            if (navigationListener != null) navigationListener.setBottomNavVisibility(View.VISIBLE);
            if (btnFullScreenAction != null) btnFullScreenAction.setImageResource(R.drawable.ic_fullscreen);
        }
    }

    private void loadPdfContent() {
        currentDownloadCall = downloadManager.loadAndSetupPdf(
                episodePdfLink,
                pdfPath,
                mainStoryTitle,
                pdfViewerController::setupPdfRenderer,
                (url) -> currentReadUrl = url
        );
    }

    @Override
    public void showLoading() {
        if (loadingLayout != null && txtLoading != null && isAdded()) {
            requireActivity().runOnUiThread(() -> {
                txtLoading.setText(getString(R.string.loading));
                loadingLayout.setVisibility(View.VISIBLE);
            });
        }
    }

    @Override
    public void hideLoading() {
        if (loadingLayout != null && isAdded()) {
            requireActivity().runOnUiThread(() -> loadingLayout.setVisibility(View.GONE));
        }
    }

    @Override
    public void hideDownloadProgress() {
        if (progressDownload != null && isAdded()) {
            requireActivity().runOnUiThread(() -> progressDownload.setVisibility(View.GONE));
        }
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
            try {
                pdfViewerController.closeRenderer();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi đóng PdfRenderer trong onDestroyView: " + e.getMessage());
            }
        }
        if (downloadManager != null) downloadManager.setIsActivityDestroyed(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pdfViewerController != null) {
            try {
                pdfViewerController.closeRenderer();
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi đóng PdfRenderer trong onDestroy: " + e.getMessage());
            }
        }
    }

    public interface NavigationListener {
        void setBottomNavVisibility(int visibility);
    }

    public String getCurrentTitle() {
        return currentTitle;
    }

    public String getMainStoryTitle() {
        return mainStoryTitle;
    }
}
