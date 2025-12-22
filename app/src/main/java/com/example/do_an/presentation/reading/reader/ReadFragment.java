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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.presentation.reading.chatbot.ChatbotFragment;
import com.example.do_an.presentation.reading.note.NoteFragment;
import com.example.do_an.R;
import com.example.do_an.presentation.reading.reader.adapter.PdfPageAdapter;
import com.example.do_an.presentation.reading.settings.SettingsManager;
import com.example.do_an.presentation.reading.settings.SpeechController;
import com.example.do_an.presentation.library.downloads.DownloadInteractor;
import com.example.do_an.domain.reading.repository.HistoryManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import okhttp3.Call;

import com.example.do_an.core.database.AppDatabase;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;

public class ReadFragment extends Fragment implements DownloadInteractor.LoadingListener {

    private static final String TAG = "ReadFragment";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;

    private TextView txtTieuDe, txtPageIndicator, txtLoading;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite, btnNote, btnFullScreenAction, btnSettings, btnChatbot;
    private LinearLayout topBar, rootLayout, loadingLayout;
    private ProgressBar progressDownload;
    private Switch switchVoiceControl;
    private View settingsContainer;

    private String userEmail;
    private String currentStoryId, currentTitle, mainStoryTitle;
    private String currentAuthor, currentCategory, currentImageUrl;
    private String currentReadUrl = "";
    private String episodePdfLink, pdfPath;

    private boolean isFullScreenMode = false;

    private SettingsManager settingsManager;
    private PdfViewerController pdfViewerController;
    private DownloadInteractor downloadInteractor;
    private FavoriteHandler favoriteHandler;
    private HistoryManager historyManager;
    private SpeechController speechController;

    private DownloadedPdfDao pdfDao;
    private Call currentDownloadCall;
    private NavigationListener navigationListener;

    private PdfOcrManager ocrManager;
    private String cachedOcrText = "";
    /* ================= FACTORY ================= */

    public static ReadFragment newInstance(Bundle args) {
        ReadFragment fragment = new ReadFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /* ================= LIFE CYCLE ================= */

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
        setupIntentData(getArguments());
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

        initViews(view);
        initManagers();
        setupPdfController();
        setupViewsAndListeners(view);

        if (pdfPath != null && !pdfPath.isEmpty()) {
            findAndSetupStoryInfoFromRoom(pdfPath);
        } else {
            checkMandatoryStoryInfo();
            loadPdfContent();
            saveStartHistory();
        }

        if (settingsManager.isVoiceControlEnabled()) {
            setupVoiceControlIfEnabled();
        }
    }

    /* ================= INIT ================= */

    private void initViews(View view) {
        txtTieuDe = view.findViewById(R.id.txtTieuDe);
        txtPageIndicator = view.findViewById(R.id.txtPageIndicator);
        txtLoading = view.findViewById(R.id.txtLoading);

        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnNote = view.findViewById(R.id.btnNote);
        btnFullScreenAction = view.findViewById(R.id.btnfull);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnChatbot = view.findViewById(R.id.btnChatbot); // Khởi tạo nút AI

        topBar = view.findViewById(R.id.topBar);
        rootLayout = (LinearLayout) view;
        loadingLayout = view.findViewById(R.id.loadingLayout);
        progressDownload = view.findViewById(R.id.progressDownload);

        settingsContainer = view.findViewById(R.id.settingsContainer);
        switchVoiceControl = view.findViewById(R.id.switchVoiceControl);

        ocrManager = new PdfOcrManager();

        if (currentTitle != null) txtTieuDe.setText(currentTitle);
    }

    private void initManagers() {
        Context ctx = requireContext();
        settingsManager = new SettingsManager(ctx);
        historyManager = new HistoryManager(ctx);
        favoriteHandler = new FavoriteHandler(ctx);

        AppDatabase db = AppDatabase.getDatabase(ctx);
        pdfDao = db.downloadedPdfDao();
        downloadInteractor = new DownloadInteractor(ctx, pdfDao);
        downloadInteractor.setLoadingListener(this);
        downloadInteractor.setTxtPageIndicator(txtPageIndicator);
    }

    private void setupPdfController() {
        pdfViewerController = new PdfViewerController(
                requireContext(),
                pdfViewPager,
                txtTieuDe,
                settingsManager,
                txtPageIndicator,
                this::getCurrentTitle,
                url -> currentReadUrl = url
        );
        pdfViewPager.registerOnPageChangeCallback(
                pdfViewerController.getPageChangeCallback()
        );
    }

    /* ================= UI + SETTINGS + AI ================= */

    private void setupViewsAndListeners(View root) {

        View btnDown = root.findViewById(R.id.btnDown);
        if (btnDown != null) {
            btnDown.setOnClickListener(v -> {
                if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                    Toast.makeText(getContext(), "Không tìm thấy link để tải", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Hiển thị tiến trình tải
                if (progressDownload != null) progressDownload.setVisibility(View.VISIBLE);

                // Tạo tên file lưu trữ: "Tên truyện - Tên tập.pdf"
                String fileName = mainStoryTitle + " - " + currentTitle + ".pdf";

                // Gọi Manager thực hiện tải xuống và lưu vào Room database
                downloadInteractor.downloadPdfWithOkHttp(
                        currentReadUrl,
                        fileName,
                        currentStoryId,
                        currentAuthor,
                        currentImageUrl
                );
            });
        }

        root.findViewById(R.id.btnBack)
                .setOnClickListener(v -> requireActivity().onBackPressed());


        favoriteHandler.checkIfFavorite(
                currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite
        );

        btnFavorite.setOnClickListener(v ->
                favoriteHandler.toggleFavorite(
                        userEmail, currentStoryId, mainStoryTitle, currentTitle,
                        currentAuthor, currentCategory, currentImageUrl,
                        currentReadUrl, btnFavorite
                )
        );

        btnChatbot.setOnClickListener(v -> {
            ocrCurrentPage();
        });

        btnNote.setOnClickListener(v -> {
            int page = pdfViewerController.getCurrentPage() + 1;
            NoteFragment f = NoteFragment.newInstance(
                    currentStoryId + "_" + currentTitle,
                    page,
                    currentTitle
            );
            getParentFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, f)
                    .addToBackStack(null)
                    .commit();
        });

        btnFullScreenAction.setOnClickListener(v -> toggleFullScreenMode());

        pdfViewerController.setupSettingsView(
                settingsContainer,
                root.findViewById(R.id.btnCloseSettings),
                btnSettings
        );

        switchVoiceControl.setChecked(settingsManager.isVoiceControlEnabled());
        switchVoiceControl.setOnCheckedChangeListener((b, checked) -> {
            settingsManager.setVoiceControl(checked);
            if (checked) setupVoiceControlIfEnabled();
            else if (speechController != null) speechController.stop();
        });
    }

    /* ================= FULL SCREEN ================= */

    private void toggleFullScreenMode() {
        isFullScreenMode = !isFullScreenMode;
        int paddingBottom = (int) (getResources().getDisplayMetrics().density * 47);

        if (isFullScreenMode) {
            topBar.setVisibility(View.GONE);
            rootLayout.setPadding(0, 0, 0, 0);
            navigationListener.setBottomNavVisibility(View.GONE);
            btnFullScreenAction.setImageResource(R.drawable.ic_exit_full);
        } else {
            topBar.setVisibility(View.VISIBLE);
            rootLayout.setPadding(0, 0, 0, paddingBottom);
            navigationListener.setBottomNavVisibility(View.VISIBLE);
            btnFullScreenAction.setImageResource(R.drawable.ic_fullscreen);
        }
    }

    /* ================= VOICE CONTROL ================= */

    private void setupVoiceControlIfEnabled() {
        if (speechController == null)
            speechController = new SpeechController(requireContext(), settingsManager);

        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_AUDIO_PERMISSION
            );
        } else {
            startVoiceControl();
        }
    }

    private void startVoiceControl() {
        speechController.startListening(command -> {
            String cmd = command.toLowerCase().trim();
            Log.d(TAG, "Voice: " + cmd);

            requireActivity().runOnUiThread(() -> {

                if (cmd.contains("mở ghi chú") || cmd.contains("Thêm ghi chú")) btnNote.performClick();
                else if (cmd.contains("đóng ghi chú")) getParentFragmentManager().popBackStack();
                else if (cmd.contains("toàn màn hình") && !isFullScreenMode) toggleFullScreenMode();
                else if (cmd.contains("thoát toàn màn hình") && isFullScreenMode) toggleFullScreenMode();
                else if (cmd.contains("tiếp") || cmd.contains("Sau")) pdfViewPager.setCurrentItem(pdfViewerController.getCurrentPage() + 1, true);
                else if (cmd.contains("trước")) pdfViewPager.setCurrentItem(pdfViewerController.getCurrentPage() - 1, true);
                else if (cmd.contains("tải xuống") || cmd.contains("tải về") || cmd.contains("download")) {
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
                else if (cmd.contains("bỏ yêu thích") || cmd.contains("xóa yêu thích")) {
                    if (btnFavorite.isSelected()) {
                        btnFavorite.performClick();
                    }
                }
                else if (cmd.contains("yêu thích")) {
                    if (!btnFavorite.isSelected()) {
                        btnFavorite.performClick();
                    }
                }
                else if (cmd.contains("quay lại")) requireActivity().onBackPressed();
            });
        });
    }

    /* ================= DATA ================= */

    private void setupIntentData(Bundle args) {
        if (args == null) return;
        episodePdfLink = args.getString("PDF_LINK");
        pdfPath = args.getString("PDF_PATH");
        currentStoryId = args.getString("STORY_ID");
        mainStoryTitle = args.getString("STORY_TITLE");
        currentAuthor = args.getString("STORY_AUTHOR");
        currentImageUrl = args.getString("STORY_IMAGE_URL");

        // --- SỬA ĐOẠN NÀY ---
        // Cố gắng lấy chính xác tên tập
        String tapName = args.getString("TAP_TITLE");

        // Kiểm tra dự phòng: Đôi khi key có thể là "TAP" thay vì "TAP_TITLE"
        if (tapName == null || tapName.isEmpty()) {
            tapName = args.getString("TAP");
        }

        if (tapName != null && !tapName.isEmpty()) {
            currentTitle = tapName;
        } else {
            // Nếu vẫn không có tên tập, thì đặt tạm 1 tên để phân biệt
            currentTitle = "Tập mới nhất";
        }
        // --------------------
    }

    private void checkMandatoryStoryInfo() {
        if (currentStoryId == null) currentStoryId = currentTitle;
        if (currentAuthor == null) currentAuthor = getString(R.string.author_unknown);
    }

    private void loadPdfContent() {
        currentDownloadCall = downloadInteractor.loadAndSetupPdf(
                episodePdfLink,
                pdfPath,
                mainStoryTitle,
                pdfViewerController::setupPdfRenderer,
                url -> currentReadUrl = url
        );
    }

    private void saveStartHistory() {
        historyManager.saveStartReadingHistory(
                userEmail,
                currentStoryId,
                mainStoryTitle,
                currentTitle,
                currentAuthor,
                currentImageUrl
        );
    }

    private void findAndSetupStoryInfoFromRoom(String path) {
        showLoading();
        new Thread(() -> {
            DownloadedPdfEntity e = pdfDao.getPdfByFilePath(path);
            requireActivity().runOnUiThread(() -> {
                if (e != null) {
                    currentStoryId = e.storyDocumentId;

                    String fileName = e.fileName.replace(".pdf", "");

                    if (fileName.contains(" - ")) {
                        String[] parts = fileName.split(" - ", 2);
                        mainStoryTitle = parts[0].trim(); // Tên Truyện
                        currentTitle = parts[1].trim();   // Tên Tập (Vd: Tập 1)
                    } else {
                        mainStoryTitle = fileName;
                        currentTitle = "Tập đã tải";
                    }
                    // --------------------

                    currentReadUrl = e.pdfUrl;

                    // Cập nhật lại Text trên giao diện để người dùng thấy tên Tập
                    if (txtTieuDe != null) txtTieuDe.setText(currentTitle);

                    loadPdfContent();
                }
                hideLoading();
            });
        }).start();
    }

    /* ================= LOADING ================= */

    @Override public void showLoading() { if (loadingLayout != null) loadingLayout.setVisibility(View.VISIBLE); }
    @Override public void hideLoading() { if (loadingLayout != null) loadingLayout.setVisibility(View.GONE); }
    @Override public void hideDownloadProgress() { if (progressDownload != null) progressDownload.setVisibility(View.GONE); }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (speechController != null) speechController.shutdown();
        if (currentDownloadCall != null) currentDownloadCall.cancel();
    }

    public String getCurrentTitle() { return currentTitle; }

    public interface NavigationListener {
        void setBottomNavVisibility(int visibility);
    }

    private void ocrCurrentPage() {

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

        if (bitmap == null) {
            Toast.makeText(getContext(), "Không lấy được ảnh trang", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading();
        txtLoading.setText("Đang phân tích trang...");

        ocrManager.ocr(bitmap, new PdfOcrManager.Callback() {
            @Override
            public void onSuccess(String text) {
                hideLoading();
                cachedOcrText = text;

                Log.d("OCR", text);

                Toast.makeText(
                        getContext(),
                        "Đã phân tích xong nội dung trang",
                        Toast.LENGTH_SHORT
                ).show();

                /* ===== MỞ CHATBOT ===== */
                ChatbotFragment chatbotFragment =
                        ChatbotFragment.newInstance(cachedOcrText);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, chatbotFragment)
                        .addToBackStack(null)
                        .commit();

            }

            @Override
            public void onError(Exception e) {
                hideLoading();
                Toast.makeText(
                        getContext(),
                        "OCR thất bại",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

}