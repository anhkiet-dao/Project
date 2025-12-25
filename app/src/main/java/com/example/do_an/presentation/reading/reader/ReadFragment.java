package com.example.do_an.presentation.reading.reader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.presentation.reading.chatbot.ChatbotFragment;
import com.example.do_an.presentation.reading.note.NoteFragment;
import com.example.do_an.presentation.library.series.SeriesFragment;
import com.example.do_an.R;
import com.example.do_an.presentation.reading.reader.adapter.PdfPageAdapter;
import com.example.do_an.presentation.reading.reader.util.FullScreenManager;
import com.example.do_an.presentation.reading.reader.util.OcrProcessor;
import com.example.do_an.presentation.reading.reader.util.ReadFragmentDataExtractor;
import com.example.do_an.presentation.reading.reader.util.VoiceCommandHandler;
import com.example.do_an.presentation.reading.settings.SettingsManager;
import com.example.do_an.presentation.reading.settings.SpeechController;
import com.example.do_an.presentation.library.downloads.DownloadController;
import com.example.do_an.domain.reading.repository.HistoryManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import com.example.do_an.core.database.AppDatabase;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;

public class ReadFragment extends Fragment implements DownloadController.LoadingListener {

    private static final String TAG = "ReadFragment";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;

    private TextView textTitle, textLoading;
    private View progressContainer;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite, btnNote, btnSettings;
    private LinearLayout topBar, loadingLayout;
    private FrameLayout rootLayout;
    private View bottomControlsContainer;
    private ProgressBar progressDownload;
    private Switch switchVoiceControl;
    private View settingsContainer;

    private String userEmail;
    private String currentReadUrl = "";
    private String currentCategory;

    private SettingsManager settingsManager;
    private PdfViewerController pdfViewerController;
    private DownloadController downloadController;
    private FavoriteHandler favoriteHandler;
    private HistoryManager historyManager;
    private SpeechController speechController;

    private ReadFragmentDataExtractor dataExtractor;
    private VoiceCommandHandler voiceCommandHandler;
    private OcrProcessor ocrProcessor;

    private boolean isControlsVisible = false;

    private DownloadedPdfDao pdfDao;
    private Call currentDownloadCall;
    private NavigationListener navigationListener;

    private String cachedOcrText = "";

    /* ================= FACTORY ================= */

    public static ReadFragment newInstance(Bundle args) {
        ReadFragment fragment = new ReadFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /* ================= LIFECYCLE ================= */

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        navigationListener = (NavigationListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }
        userEmail = user.getEmail();

        dataExtractor = new ReadFragmentDataExtractor();
        dataExtractor.extractFromBundle(getArguments());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_activity_reading, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        initDependencies();
        setupUi();
        bindActions(view);

        if (dataExtractor.getPdfPath() != null && !dataExtractor.getPdfPath().isEmpty()) {
            loadPdfFromDb(dataExtractor.getPdfPath());
        } else {
            validateStoryInfo();
            doLoadPdf();
            doSaveHistory();
        }

        if (settingsManager.isVoiceControlEnabled()) {
            setupVoiceControl();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechController != null)
            speechController.shutdown();
        if (currentDownloadCall != null)
            currentDownloadCall.cancel();

        // Restore bottom nav when leaving reading screen
        showBottomNav();
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews(View view) {
        textTitle = view.findViewById(R.id.txtTieuDe);
        progressContainer = view.findViewById(R.id.progressContainer);
        textLoading = view.findViewById(R.id.txtLoading);

        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnNote = view.findViewById(R.id.btnNote);
        btnSettings = view.findViewById(R.id.btnSettings);

        topBar = view.findViewById(R.id.topBar);
        bottomControlsContainer = view.findViewById(R.id.bottomControlsContainer);
        rootLayout = (FrameLayout) view;
        loadingLayout = view.findViewById(R.id.loadingLayout);
        progressDownload = view.findViewById(R.id.progressDownload);

        settingsContainer = view.findViewById(R.id.settingsContainer);
        switchVoiceControl = view.findViewById(R.id.switchVoiceControl);
    }

    private void initDependencies() {
        Context ctx = requireContext();
        settingsManager = new SettingsManager(ctx);
        historyManager = new HistoryManager(ctx);
        favoriteHandler = new FavoriteHandler(ctx);

        AppDatabase db = AppDatabase.getDatabase(ctx);
        pdfDao = db.downloadedPdfDao();
        downloadController = new DownloadController(ctx, pdfDao);
        downloadController.setLoadingListener(this);
        downloadController.setProgressContainer(progressContainer);

        voiceCommandHandler = new VoiceCommandHandler(createVoiceCommandCallback());
        ocrProcessor = new OcrProcessor(createOcrCallback());
    }

    private void setupUi() {
        if (dataExtractor.getCurrentTitle() != null) {
            textTitle.setText(dataExtractor.getCurrentTitle());
        }
        setupPdfController();

        // Start in immersive mode and hide bottom nav
        hideControls();
        hideBottomNav();

        // Setup tap to toggle using touch listener
        setupTapToToggle();
    }

    private void setupTapToToggle() {
        // Use circular button in center to toggle controls
        View btnToggle = getView().findViewById(R.id.btnToggleControls);
        btnToggle.setOnClickListener(v -> toggleControls());
    }

    private void bindActions(View root) {
        View btnDown = root.findViewById(R.id.btnDown);
        View btnBack = root.findViewById(R.id.btnBack);
        View btnCloseSettings = root.findViewById(R.id.btnCloseSettings);

        if (btnDown != null) {
            btnDown.setOnClickListener(v -> onDownloadIntent());
        }

        btnBack.setOnClickListener(v -> onBackIntent());
        btnFavorite.setOnClickListener(v -> onFavoriteIntent());
        btnNote.setOnClickListener(v -> onNoteIntent());

        pdfViewerController.setupSettingsView(settingsContainer, (AppCompatButton) btnCloseSettings, btnSettings);

        switchVoiceControl.setChecked(settingsManager.isVoiceControlEnabled());
        switchVoiceControl.setOnCheckedChangeListener((b, checked) -> onVoiceControlToggled(checked));

        favoriteHandler.checkIfFavorite(
                dataExtractor.getCurrentStoryId(),
                dataExtractor.getMainStoryTitle(),
                dataExtractor.getCurrentTitle(),
                userEmail,
                btnFavorite);
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void setupPdfController() {
        pdfViewerController = new PdfViewerController(
                requireContext(),
                pdfViewPager,
                textTitle,
                settingsManager,
                progressContainer,
                this::getCurrentTitle,
                url -> currentReadUrl = url);
        pdfViewPager.registerOnPageChangeCallback(
                pdfViewerController.getPageChangeCallback());
    }

    @Override
    public void showLoading() {
        if (loadingLayout != null)
            loadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        if (loadingLayout != null)
            loadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void hideDownloadProgress() {
        if (progressDownload != null)
            progressDownload.setVisibility(View.GONE);
    }

    private void showDownloadProgress() {
        if (progressDownload != null)
            progressDownload.setVisibility(View.VISIBLE);
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        isControlsVisible = true;
        topBar.setVisibility(View.VISIBLE);
        bottomControlsContainer.setVisibility(View.VISIBLE);
    }

    private void hideControls() {
        isControlsVisible = false;
        topBar.setVisibility(View.GONE);
        bottomControlsContainer.setVisibility(View.GONE);
    }

    private void hideBottomNav() {
        if (navigationListener != null) {
            navigationListener.setBottomNavVisibility(View.GONE);
        }
    }

    private void showBottomNav() {
        if (navigationListener != null) {
            navigationListener.setBottomNavVisibility(View.VISIBLE);
        }
    }

    // =========================================================
    // 3️⃣ Intent handlers
    // =========================================================

    private void onDownloadIntent() {
        if (currentReadUrl == null || currentReadUrl.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.pdf_download_missing),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showDownloadProgress();

        String fileName = dataExtractor.getMainStoryTitle() + " - " + dataExtractor.getCurrentTitle() + ".pdf";

        downloadController.downloadPdfWithOkHttp(
                currentReadUrl,
                fileName,
                dataExtractor.getCurrentStoryId(),
                dataExtractor.getCurrentAuthor(),
                dataExtractor.getCurrentImageUrl());
    }

    private void onBackIntent() {
        // If opened from Favorite, just go back
        if (getArguments() != null && getArguments().getBoolean("IS_FROM_FAVORITE", false)) {
            getParentFragmentManager().popBackStack();
            return;
        }

        // Navigate to SeriesFragment to show chapter list
        Bundle args = new Bundle();
        args.putString("STORY_NAME", dataExtractor.getMainStoryTitle());
        args.putString("STORY_AUTHOR", dataExtractor.getCurrentAuthor());
        args.putString("STORY_ID", dataExtractor.getCurrentStoryId());
        args.putString("STORY_IMAGE_URL", dataExtractor.getCurrentImageUrl());

        SeriesFragment seriesFragment = SeriesFragment.newInstance(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, seriesFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onFavoriteIntent() {
        favoriteHandler.toggleFavorite(
                userEmail,
                dataExtractor.getCurrentStoryId(),
                dataExtractor.getMainStoryTitle(),
                dataExtractor.getCurrentTitle(),
                dataExtractor.getCurrentAuthor(),
                currentCategory,
                dataExtractor.getCurrentImageUrl(),
                currentReadUrl,
                btnFavorite);
    }

    private void onChatbotIntent() {
        doOcr();
    }

    private void onNoteIntent() {
        int page = pdfViewerController.getCurrentPage() + 1;
        NoteFragment f = NoteFragment.newInstance(
                dataExtractor.getCurrentStoryId() + "_" + dataExtractor.getCurrentTitle(),
                page,
                dataExtractor.getCurrentTitle());
        getParentFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, f)
                .addToBackStack(null)
                .commit();
    }

    private void onVoiceControlToggled(boolean enabled) {
        settingsManager.setVoiceControl(enabled);
        if (enabled) {
            setupVoiceControl();
        } else if (speechController != null) {
            speechController.stop();
        }
    }

    // =========================================================
    // 4️⃣ Data validation
    // =========================================================

    private void validateStoryInfo() {
        if (dataExtractor.getCurrentStoryId() == null) {
            dataExtractor.setCurrentStoryId(dataExtractor.getCurrentTitle());
        }
        if (dataExtractor.getCurrentAuthor() == null) {
            dataExtractor.setCurrentAuthor(getString(R.string.author_unknown));
        }
    }

    // =========================================================
    // 5️⃣ Business actions
    // =========================================================

    private void doLoadPdf() {
        currentDownloadCall = downloadController.loadAndSetupPdf(
                dataExtractor.getEpisodePdfLink(),
                dataExtractor.getPdfPath(),
                dataExtractor.getMainStoryTitle(),
                pdfViewerController::setupPdfRenderer,
                url -> currentReadUrl = url);
    }

    private void doSaveHistory() {
        historyManager.saveStartReadingHistory(
                userEmail,
                dataExtractor.getCurrentStoryId(),
                dataExtractor.getMainStoryTitle(),
                dataExtractor.getCurrentTitle(),
                dataExtractor.getCurrentAuthor(),
                dataExtractor.getCurrentImageUrl());
    }

    private void loadPdfFromDb(String path) {
        showLoading();
        new Thread(() -> {
            DownloadedPdfEntity e = pdfDao.getPdfByFilePath(path);
            requireActivity().runOnUiThread(() -> {
                if (e != null) {
                    onLocalPdfLoaded(e);
                }
                hideLoading();
            });
        }).start();
    }

    private void doOcr() {
        if (pdfViewerController == null) {
            Toast.makeText(getContext(), "PDF chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        int pageIndex = pdfViewerController.getCurrentPage();

        RecyclerView.Adapter<?> adapter = pdfViewPager.getAdapter();
        if (!(adapter instanceof PdfPageAdapter)) {
            Toast.makeText(getContext(), "Không lấy được trang PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfPageAdapter pdfAdapter = (PdfPageAdapter) adapter;
        Bitmap bitmap = pdfAdapter.getPageBitmap(pageIndex);

        ocrProcessor.processBitmap(bitmap, textLoading);
    }

    // =========================================================
    // 6️⃣ Result handlers
    // =========================================================

    private void onLocalPdfLoaded(DownloadedPdfEntity entity) {
        dataExtractor.setCurrentStoryId(entity.storyDocumentId);

        String fileName = entity.fileName.replace(".pdf", "");

        if (fileName.contains(" - ")) {
            String[] parts = fileName.split(" - ", 2);
            dataExtractor.setMainStoryTitle(parts[0].trim());
            dataExtractor.setCurrentTitle(parts[1].trim());
        } else {
            dataExtractor.setMainStoryTitle(fileName);
            dataExtractor.setCurrentTitle("Tập đã tải");
        }

        currentReadUrl = entity.pdfUrl;

        if (textTitle != null) {
            textTitle.setText(dataExtractor.getCurrentTitle());
        }

        doLoadPdf();
    }

    // =========================================================
    // 7️⃣ Voice control
    // =========================================================

    private void setupVoiceControl() {
        if (speechController == null)
            speechController = new SpeechController(requireContext(), settingsManager);

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    REQUEST_AUDIO_PERMISSION);
        } else {
            startVoiceListening();
        }
    }

    private void startVoiceListening() {
        speechController.startListening(command -> {
            Log.d(TAG, "Voice: " + command);
            requireActivity().runOnUiThread(() -> {
                voiceCommandHandler.handleCommand(command);
            });
        });
    }

    private VoiceCommandHandler.VoiceCommandCallback createVoiceCommandCallback() {
        return new VoiceCommandHandler.VoiceCommandCallback() {
            @Override
            public void onOpenNote() {
                btnNote.performClick();
            }

            @Override
            public void onCloseNote() {
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onToggleFullScreen() {
                toggleControls();
            }

            @Override
            public void onNextPage() {
                pdfViewPager.setCurrentItem(pdfViewerController.getCurrentPage() + 1, true);
            }

            @Override
            public void onPreviousPage() {
                pdfViewPager.setCurrentItem(pdfViewerController.getCurrentPage() - 1, true);
            }

            @Override
            public void onDownload() {
                if (getView() != null) {
                    View btnDown = getView().findViewById(R.id.btnDown);
                    if (btnDown != null) {
                        btnDown.performClick();
                        Toast.makeText(getContext(), "Đang bắt đầu tải...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Không tìm thấy nút tải", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onToggleFavorite() {
                btnFavorite.performClick();
            }

            @Override
            public void onBack() {
                requireActivity().onBackPressed();
            }

            @Override
            public boolean isFullScreen() {
                return !isControlsVisible;
            }

            @Override
            public boolean isFavorite() {
                return btnFavorite.isSelected();
            }
        };
    }

    private OcrProcessor.OcrCallback createOcrCallback() {
        return new OcrProcessor.OcrCallback() {
            @Override
            public void onStart() {
                showLoading();
            }

            @Override
            public void onSuccess(String text) {
                hideLoading();
                cachedOcrText = text;
                Log.d("OCR", text);
                Toast.makeText(getContext(), "Đã phân tích xong nội dung trang", Toast.LENGTH_SHORT).show();
                navigateToChatbot(cachedOcrText);
            }

            @Override
            public void onError(String message) {
                hideLoading();
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        };
    }

    // =========================================================
    // 8️⃣ Navigation
    // =========================================================

    private void navigateToChatbot(String ocrText) {
        ChatbotFragment chatbotFragment = ChatbotFragment.newInstance(ocrText);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, chatbotFragment)
                .addToBackStack(null)
                .commit();
    }

    // =========================================================
    // 9️⃣ Public interface
    // =========================================================

    public String getCurrentTitle() {
        return dataExtractor.getCurrentTitle();
    }

    public interface NavigationListener {
        void setBottomNavVisibility(int visibility);
    }
}