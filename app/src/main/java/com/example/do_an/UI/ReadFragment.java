package com.example.do_an.UI;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
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
import androidx.viewpager2.widget.ViewPager2;

import com.example.do_an.Note.NoteFragment;
import com.example.do_an.R;
import com.example.do_an.application.SettingsManager;
import com.example.do_an.application.SpeechController;
import com.example.do_an.Download.DownloadManager;
import com.example.do_an.Favorite.FavoriteHandler;
import com.example.do_an.History.HistoryManager;
import com.example.do_an.pdf.PdfViewerController;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.Objects;

import okhttp3.Call;

import com.example.do_an.data.AppDatabase;
import com.example.do_an.data.DownloadedPdfDao;
import com.example.do_an.data.DownloadedPdfEntity;

public class ReadFragment extends Fragment implements DownloadManager.LoadingListener {

    private static final String TAG = "ReadFragment";
    private static final int REQUEST_AUDIO_PERMISSION = 1001;

    private TextView txtTieuDe, txtPageIndicator, txtLoading;
    private Switch switchVoiceControl;
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
    private SpeechController speechController;

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
            throw new ClassCastException(context.toString() + " must implement NavigationListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(requireContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            if (getActivity() != null) getActivity().finish();
        }

        setupIntentData(getArguments());

        if (currentStoryId == null && pdfPath == null) {
            Toast.makeText(requireContext(), getString(R.string.no_story_data), Toast.LENGTH_LONG).show();
            if (getActivity() != null) getActivity().finish();
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

        initViews(view);
        initManagers();
        setupPdfController();
        setupViewsAndListeners(view);

        if (pdfPath != null && !pdfPath.trim().isEmpty()) {
            findAndSetupStoryInfoFromRoom(pdfPath);
        } else {
            checkMandatoryStoryInfo();
            loadPdfContent();
            saveStartHistory();
        }

        // Voice Control khởi tạo
        if (settingsManager.isVoiceControlEnabled()) {
            setupVoiceControlIfEnabled();
        }
    }

    private void initViews(View view) {
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
    }

    private void initManagers() {
        Context context = requireContext();
        settingsManager = new SettingsManager(context);
        historyManager = new HistoryManager(context);
        favoriteHandler = new FavoriteHandler(context);

        AppDatabase db = AppDatabase.getDatabase(context);
        pdfDao = db.downloadedPdfDao();
        downloadManager = new DownloadManager(context, pdfDao);
        downloadManager.setLoadingListener(this);
        downloadManager.setTxtPageIndicator(txtPageIndicator);
    }

    private void setupPdfController() {
        pdfViewerController = new PdfViewerController(
                requireContext(), pdfViewPager, txtTieuDe, settingsManager,
                txtPageIndicator, this::getCurrentTitle, url -> currentReadUrl = url
        );
        pdfViewPager.registerOnPageChangeCallback(pdfViewerController.getPageChangeCallback());
    }

    private void setupVoiceControlIfEnabled() {
        if (speechController == null) {
            speechController = new SpeechController(requireContext(), settingsManager);
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        } else {
            startVoiceControl();
        }
    }

    private void startVoiceControl() {
        if (speechController == null) return;

        speechController.startListening(command -> {
            final String cmd = command.toLowerCase().trim();
            Log.d(TAG, "Lệnh nhận: " + cmd);

            requireActivity().runOnUiThread(() -> {
                // 1. LỆNH ĐIỀU HƯỚNG TRANG
                if (cmd.contains("tiếp") || cmd.contains("sang trang")) {
                    int currentPage = pdfViewerController.getCurrentPage();
                    if (pdfViewPager.getAdapter() != null && currentPage + 1 < pdfViewPager.getAdapter().getItemCount()) {
                        pdfViewPager.setCurrentItem(currentPage + 1, true);
                    }
                }
                else if (cmd.contains("trước") || cmd.contains("lùi")) {
                    int currentPage = pdfViewerController.getCurrentPage();
                    if (currentPage - 1 >= 0) {
                        pdfViewPager.setCurrentItem(currentPage - 1, true);
                    }
                }

                // 2. LỆNH GHI CHÚ (NOTE)
                else if (cmd.contains("mở ghi chú") || cmd.contains("tạo ghi chú")) {
                    btnNote.performClick(); // Mở NoteFragment
                }
                else if (cmd.contains("đóng ghi chú") || cmd.contains("thoát ghi chú")) {
                    // Kiểm tra nếu NoteFragment đang hiển thị thì đóng lại
                    Fragment noteFrag = getParentFragmentManager().findFragmentById(R.id.fragment_container);
                    if (noteFrag instanceof NoteFragment) {
                        getParentFragmentManager().popBackStack();
                    }
                }

                // 3. LỆNH YÊU THÍCH (FAVORITE)
                else if (cmd.contains("thêm yêu thích") || cmd.contains("thích truyện")) {
                    // Kiểm tra trạng thái nếu chưa yêu thích thì mới click
                    if (btnFavorite.getTag() == null || !(boolean)btnFavorite.getTag()) {
                        btnFavorite.performClick();
                        speechController.speak("Đã thêm vào yêu thích");
                    }
                }
                else if (cmd.contains("bỏ yêu thích") || cmd.contains("không thích nữa")) {
                    if (btnFavorite.getTag() != null && (boolean)btnFavorite.getTag()) {
                        btnFavorite.performClick();
                        speechController.speak("Đã bỏ yêu thích");
                    }
                }

                // 4. LỆNH TOÀN MÀN HÌNH
                else if (cmd.contains("toàn màn hình") || cmd.contains("thoát toàn màn hình")) {
                    toggleFullScreenMode();
                }

                // 5. LỆNH TẢI XUỐNG
                else if (cmd.contains("tải xuống") || cmd.contains("download")) {
                    View btnDown = getView().findViewById(R.id.btnDown);
                    if (btnDown != null) btnDown.performClick();
                }

                // Gợi ý: Để Xóa/Sửa ghi chú cụ thể bằng giọng nói,
                // bạn nên thực hiện trong NoteFragment vì ở đó mới có danh sách ID ghi chú.
            });
        });
    }

    private void toggleFullScreenMode() {
        isFullScreenMode = !isFullScreenMode;
        int bottomPadding = (int) (getResources().getDisplayMetrics().density * 47);
        if (isFullScreenMode) {
            if (topBar != null) topBar.setVisibility(View.GONE);
            if (navigationListener != null) navigationListener.setBottomNavVisibility(View.GONE);
            if (rootLayout != null) rootLayout.setPadding(0, 0, 0, 0);
            if (btnFullScreenAction != null) btnFullScreenAction.setImageResource(R.drawable.ic_exit_full);
        } else {
            if (topBar != null) topBar.setVisibility(View.VISIBLE);
            if (navigationListener != null) navigationListener.setBottomNavVisibility(View.VISIBLE);
            if (rootLayout != null) rootLayout.setPadding(0, 0, 0, bottomPadding);
            if (btnFullScreenAction != null) btnFullScreenAction.setImageResource(R.drawable.ic_fullscreen);
        }
    }

    private void setupViewsAndListeners(View root) {
        root.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        favoriteHandler.checkIfFavorite(currentStoryId, mainStoryTitle, currentTitle, userEmail, btnFavorite);
        btnFavorite.setOnClickListener(v -> favoriteHandler.toggleFavorite(userEmail, currentStoryId,
                mainStoryTitle, currentTitle, currentAuthor, currentCategory, currentImageUrl, currentReadUrl, btnFavorite));

        root.findViewById(R.id.btnDown).setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(getContext(), "Không có link tải", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDownload.setVisibility(View.VISIBLE);
            String fullFileName = mainStoryTitle + " - " + currentTitle + ".pdf";
            downloadManager.downloadPdfWithOkHttp(currentReadUrl, fullFileName, currentStoryId, currentAuthor, currentImageUrl);
        });

        btnNote.setOnClickListener(v -> {
            int currentPage = pdfViewerController.getCurrentPage() + 1;
            NoteFragment noteFragment = NoteFragment.newInstance(currentStoryId + "_" + currentTitle, currentPage, currentTitle);
            getParentFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, noteFragment)
                    .addToBackStack(null).commit();
        });

        btnFullScreenAction.setOnClickListener(v -> toggleFullScreenMode());

        switchVoiceControl = root.findViewById(R.id.switchVoiceControl);
        switchVoiceControl.setChecked(settingsManager.isVoiceControlEnabled());
        switchVoiceControl.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsManager.setVoiceControl(isChecked);
            if (isChecked) setupVoiceControlIfEnabled();
            else if (speechController != null) speechController.stop();
        });
    }

    // Các hàm phụ trợ (checkMandatoryStoryInfo, setupIntentData, loadPdfContent, v.v.) giữ nguyên logic của bạn
    private void checkMandatoryStoryInfo() {
        if (currentStoryId == null) currentStoryId = (mainStoryTitle != null) ? mainStoryTitle : currentTitle;
        if (currentAuthor == null) currentAuthor = "Ẩn danh";
    }

    private void setupIntentData(Bundle args) {
        if (args == null) return;
        episodePdfLink = args.getString("PDF_LINK");
        pdfPath = args.getString("PDF_PATH");
        currentStoryId = args.getString("STORY_ID");
        mainStoryTitle = args.getString("STORY_TITLE");
        currentAuthor = args.getString("STORY_AUTHOR");
        currentImageUrl = args.getString("STORY_IMAGE_URL");
        String epTitle = args.getString("TAP_TITLE");
        currentTitle = (epTitle != null) ? epTitle : mainStoryTitle;
    }

    private void loadPdfContent() {
        currentDownloadCall = downloadManager.loadAndSetupPdf(
                episodePdfLink, pdfPath, mainStoryTitle,
                pdfViewerController::setupPdfRenderer, url -> currentReadUrl = url
        );
    }

    private void saveStartHistory() {
        if (userEmail != null && currentStoryId != null) {
            historyManager.saveStartReadingHistory(userEmail, currentStoryId, mainStoryTitle, currentTitle, currentAuthor, currentImageUrl);
        }
    }

    private void findAndSetupStoryInfoFromRoom(String filePath) {
        showLoading();
        new Thread(() -> {
            DownloadedPdfEntity entity = pdfDao.getPdfByFilePath(filePath);
            requireActivity().runOnUiThread(() -> {
                if (entity != null) {
                    currentStoryId = entity.storyDocumentId;
                    currentTitle = entity.fileName.replace(".pdf", "");
                    mainStoryTitle = currentTitle;
                    currentReadUrl = entity.pdfUrl;
                    loadPdfContent();
                    hideLoading();
                }
            });
        }).start();
    }

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
    public interface NavigationListener { void setBottomNavVisibility(int visibility); }
}