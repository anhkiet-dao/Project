package com.example.do_an.application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.example.do_an.R;
import com.example.do_an.Story.FavoriteManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import android.widget.ImageButton;
import android.widget.Switch;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;

public class ReadActivity extends AppCompatActivity {
    private static final String TAG = "ReadActivity";
    private TextView txtTieuDe;
    private ViewPager2 pdfViewPager;
    private ImageView btnFavorite;
    private FirebaseFirestore db;
    private FavoriteManager favoriteManager;
    private FirebaseUser currentUser;
    private PdfPageAdapter pdfPageAdapter;
    private boolean isFavorite = false;
    private String userEmail;
    private ImageView btnSettings, btnDownLoad, btnLibrary;
    private int currentPage = 0;
    private String currentStoryId;
    private String currentTitle;
    private String currentAuthor;
    private String currentCategory;
    private String currentDescription;
    private String currentImageUrl;
    private String currentReadUrl = "";
    private String mainStoryTitle;
    private Call currentDownloadCall;
    private boolean isActivityDestroyed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("darkMode", false);
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_reading);

        txtTieuDe = findViewById(R.id.txtTieuDe);
        pdfViewPager = findViewById(R.id.pdfViewPager);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnDownLoad = findViewById(R.id.btnDown);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        View settingsContainer = findViewById(R.id.settingsContainer);
        AppCompatButton btnCloseSettings = findViewById(R.id.btnCloseSettings);
        Switch switchDarkMode = findViewById(R.id.switch_dark_mode);
        ImageView btnSettings = findViewById(R.id.btnSettings);

        db = FirebaseFirestore.getInstance();
        favoriteManager = new FavoriteManager();

        // ✅ Lấy email người dùng đang đăng nhập
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        } else {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent intent = getIntent();

        String episodePdfLink = intent.getStringExtra("PDF_LINK");
        String episodeTitle = intent.getStringExtra("TAP");
        String pdfPath = intent.getStringExtra("PDF_PATH");

        currentStoryId = intent.getStringExtra("STORY_ID");
        currentTitle = intent.getStringExtra("STORY_TITLE");
        currentAuthor = intent.getStringExtra("STORY_AUTHOR");
        currentCategory = intent.getStringExtra("STORY_CATEGORY");
        currentImageUrl = intent.getStringExtra("STORY_IMAGE_URL");
        currentDescription = intent.getStringExtra("STORY_DESCRIPTION");
        mainStoryTitle = intent.getStringExtra("STORY_TITLE");

        if (episodeTitle != null) {
            currentTitle = episodeTitle; // Ưu tiên tên tập "Tập 01"
        } else if (pdfPath != null) {
            currentTitle = new File(pdfPath).getName().replace(".pdf", ""); // Tên file local
        } else {
            currentTitle = mainStoryTitle; // Tên truyện chính
        }

        if (currentStoryId == null || currentTitle == null) {
            Toast.makeText(this, "Lỗi: Không có thông tin truyện!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        txtTieuDe.setText(currentTitle);

        checkIfFavorite();
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnCloseSettings.setOnClickListener(v -> settingsContainer.setVisibility(View.GONE));

        btnSettings.setOnClickListener(v -> {
            settingsContainer.setVisibility(View.VISIBLE);
            switchDarkMode.setChecked(prefs.getBoolean("darkMode", false));
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        pdfViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
            }
        });

        String displayTitle = currentTitle; // Giữ lại tên hiển thị (VD: "Tập 01")
        currentTitle = mainStoryTitle != null ? mainStoryTitle : displayTitle;

        saveStartReadingHistory();

        currentTitle = displayTitle; // Trả lại tên hiển thị cho
        txtTieuDe.setText(currentTitle);

        // Kiểm tra xem có mở PDF từ DownloadedPdfActivity không
        if (episodePdfLink != null && !episodePdfLink.isEmpty()) {
            // === ƯU TIÊN 1: Tải "LINK MỚI" (Link tập) ===
            currentReadUrl = episodePdfLink; // Lưu link tập để tải/favorite
            downloadPdfToCache(currentReadUrl, "temp_episode.pdf");
        } else if (pdfPath != null) {
            // === ƯU TIÊN 2: Tải file LOCAL (từ DownloadedPdfActivity) ===
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                setupPdfRenderer(pdfFile);
                // Tìm link PDF gốc dựa trên tên truyện chính
                findAndSetCurrentReadUrl(mainStoryTitle);
            } else {
                Toast.makeText(this, "File PDF không tồn tại, tải lại...", Toast.LENGTH_SHORT).show();
                loadPdfFromFirestore(mainStoryTitle); // Tải lại "LINK CŨ"
            }
        }
        else {
            // === ƯU TIÊN 3: Tải "LINK CŨ" (Link của truyện chính) ===
            loadPdfFromFirestore(mainStoryTitle);
        }

        btnDownLoad.setOnClickListener(v -> {
            if (currentReadUrl == null || currentReadUrl.isEmpty()) {
                Toast.makeText(this, "Không tìm thấy link PDF!", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = currentTitle + ".pdf";

            downloadPdfWithOkHttp(currentReadUrl, fileName);
        });
    }

    private void downloadPdfWithOkHttp(String driveUrl, String fileName) {

        runOnUiThread(() -> Toast.makeText(this, "Bắt đầu tải dữ liệu ...", Toast.LENGTH_SHORT).show());

        new Thread(() -> {
            try {
                // Lấy fileId từ Drive
                String fileId = driveUrl.split("/d/")[1].split("/")[0];
                String downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;

                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(downloadUrl)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> Toast.makeText(this, "Không tải được PDF!", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Lấy InputStream
                InputStream is = response.body().byteStream();

                // Lưu file vào thư mục /Android/data/.../files/PDF
                File pdfDir = new File(getExternalFilesDir(null), "PDF");
                if (!pdfDir.exists()) pdfDir.mkdirs();
                File pdfFile = new File(pdfDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);

                // Buffer lớn giúp tải nhanh
                byte[] buffer = new byte[65536]; // 64 KB
                int len;
                long total = 0;
                long fileSize = response.body().contentLength();

                // Đọc & ghi file
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    total += len;
                }

                fos.flush();
                fos.close();
                is.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Tải dữ liệu thành công!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Lỗi tải PDF!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    // moi them vao 16/11/2025
    private void findAndSetCurrentReadUrl(String storyDocumentId) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String pdfUrl = doc.getString("pdfUrl");
                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            // ✅ Chỉ gán link, không download
                            currentReadUrl = pdfUrl;
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Lỗi khi tìm pdfUrl: " + e.getMessage()));
    }

    private void checkIfFavorite() {
        String mainStoryTitle = getIntent().getStringExtra("STORY_TITLE");
        String episodeTitle = getIntent().getStringExtra("TAP");

        String titleToCheck = mainStoryTitle;
        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            titleToCheck = mainStoryTitle + " - " + episodeTitle;
        }

        final String finalTitleToCheck = titleToCheck;

        favoriteManager.getFavorites(userEmail, favorites -> {
            boolean found = false; // Cờ đánh dấu tìm thấy hay chưa

            for (Map<String, Object> item : favorites) {
                if (item != null) {
                    String id = (String) item.get("storyId");
                    String title = (String) item.get("title");

                    if (currentStoryId.equals(id) && finalTitleToCheck.equals(title)) {
                        found = true;
                        break; // Tìm thấy rồi thì thoát vòng lặp luôn
                    }
                }
            }

            isFavorite = found;
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    private void toggleFavorite() {
        String mainStoryTitle = getIntent().getStringExtra("STORY_TITLE");
        String episodeTitle = getIntent().getStringExtra("TAP");
        String titleForFavorite = mainStoryTitle;

        // Nếu có tên tập, nối chuỗi lại: "Tên Truyện - Tên Tập"
        if (episodeTitle != null && !episodeTitle.isEmpty()) {
            titleForFavorite = mainStoryTitle + " - " + episodeTitle;
        }

        if (!isFavorite) {
            // Thêm yêu thích
            favoriteManager.addFavorite(
                    userEmail,
                    currentStoryId,
                    titleForFavorite,
                    currentAuthor,
                    currentCategory,
                    currentDescription,
                    currentImageUrl,
                    currentReadUrl
            );
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            Toast.makeText(this, "Đã thêm: " + titleForFavorite + " vào yêu thích ❤️", Toast.LENGTH_SHORT).show();
            isFavorite = true;
        } else {
            // ❌ SỬA Ở ĐÂY: Truyền thêm titleForFavorite để xóa đúng cái tập đó
            favoriteManager.removeFavorite(userEmail, currentStoryId, titleForFavorite);

            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            Toast.makeText(this, "Đã xóa khỏi yêu thích 💔", Toast.LENGTH_SHORT).show();
            isFavorite = false;
        }
    }

    private void loadPdfFromFirestore(String storyDocumentId) {
        db.collection("Truyentranh").document(storyDocumentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Tìm thấy truyện, lấy pdfUrl
                        String pdfUrl = doc.getString("pdfUrl");

                        if (pdfUrl != null && !pdfUrl.isEmpty()) {
                            currentReadUrl = pdfUrl; // Lưu lại để dùng cho Favorite
                            downloadPdfToCache(pdfUrl, "temp_story.pdf");
                        } else {
                            Toast.makeText(this, "Truyện này không có file PDF!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Không tìm thấy document
                        Toast.makeText(this, "Không tìm thấy dữ liệu PDF cho truyện '" + storyDocumentId + "'!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void downloadPdfToCache(String pdfUrl, String fileName) {
        runOnUiThread(() ->
                Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show()
        );

        new Thread(() -> {
            try {
                // ---- B1: Chuyển link Drive sang direct link nếu cần ----
                final String downloadUrl;
                if (pdfUrl.contains("drive.google.com/file/d/")) {
                    String fileId = pdfUrl.split("/d/")[1].split("/")[0];
                    downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
                } else {
                    downloadUrl = pdfUrl;
                }

                // ---- B2: Tạo OkHttpClient ----
                OkHttpClient client = new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build();

                Request request = new Request.Builder()
                        .url(downloadUrl)
                        .build();

                currentDownloadCall = client.newCall(request);

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Không tải thành công dữ liệu!", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // ---- B3: Lấy InputStream từ response ----
                InputStream is = response.body().byteStream();

                // ---- B4: Lưu file vào cache ----
                File cacheDir = new File(getCacheDir(), "PDF");
                if (!cacheDir.exists()) cacheDir.mkdirs();
                File pdfFile = new File(cacheDir, fileName);

                FileOutputStream fos = new FileOutputStream(pdfFile);
                byte[] buffer = new byte[64 * 1024]; // 64 KB
                int len;
                while ((len = is.read(buffer)) != -1) {
                    if (isActivityDestroyed) {
                        is.close();
                        fos.close();
                        pdfFile.delete();
                        Log.d(TAG, "Đã hủy tải do thoát màn hình");
                        return;
                    }
                    fos.write(buffer, 0, len);
                }

                fos.flush();
                fos.close();
                is.close();
                response.close();

                // ---- B5: Thông báo thành công và mở PDF ----
                runOnUiThread(() -> {
                    Toast.makeText(this, "Tải dữ liệu thành công!", Toast.LENGTH_SHORT).show();
                    setupPdfRenderer(pdfFile);
                });

            } catch (Exception e) {
                if (isActivityDestroyed || "Canceled".equals(e.getMessage())) {
                    Log.d(TAG, "Download cancelled by user exit.");
                } else {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Lỗi tải PDF!", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void setupPdfRenderer(File pdfFile) {
        try {
            pdfPageAdapter = new PdfPageAdapter(this, pdfFile);
            pdfViewPager.setAdapter(pdfPageAdapter);
            pdfViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
            txtTieuDe.setText(currentTitle + " (" + pdfPageAdapter.getItemCount() + " trang)");
            Toast.makeText(this, "Tải xong, bắt đầu đọc!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi setup PdfRenderer", e);
            Toast.makeText(this, "Không thể mở file PDF đã tải.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;

        if (currentDownloadCall != null && !currentDownloadCall.isCanceled()) {
            currentDownloadCall.cancel();
        }

        try {
            if (pdfPageAdapter != null) {
                pdfPageAdapter.close();
                Log.d(TAG, "Đã đóng PdfRenderer.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveEndReadingHistory(); // 🔹 Khi thoát hoặc đóng Activity thì lưu thời gian kết thúc
    }
    private String currentHistoryKey; // Biến toàn cục
    private void saveStartReadingHistory() {
        if (currentUser == null || currentStoryId == null) return;

        // Lấy tên truyện chính, phòng trường hợp currentTitle là tên tập
        String mainStoryTitle = getIntent().getStringExtra("STORY_TITLE");
        // Nếu tên chính null, dùng tạm currentTitle (currentTitle lúc này đã được gán là mainStoryTitle ở onCreate)
        String episodeTitle = getIntent().getStringExtra("TAP");
        String currentEpisodeTitle = (episodeTitle != null && !episodeTitle.isEmpty())
                ? episodeTitle
                : currentTitle;
        String titleForHistory = (mainStoryTitle != null) ? mainStoryTitle : currentTitle;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String startTime = sdf.format(new Date());

        HashMap<String, Object> historyData = new HashMap<>();
        historyData.put("title", Encryption.encrypt(titleForHistory)); // ⭐️ Luôn dùng tên truyện chính
        historyData.put("author", Encryption.encrypt(currentAuthor));
        historyData.put("episodeTitle", Encryption.encrypt(currentEpisodeTitle));
        historyData.put("startTime", Encryption.encrypt(startTime));
        historyData.put("storyId", Encryption.encrypt(currentStoryId));

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(userEmail.replace(".", "_"))
                .push(); // Tạo node mới
        currentHistoryKey = dbRef.getKey(); // ✅ Lưu key
        dbRef.setValue(historyData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian bắt đầu đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian bắt đầu", e));
    }

    private void saveEndReadingHistory() {
        if (currentUser == null || currentStoryId == null || currentHistoryKey == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String endTime = Encryption.encrypt(sdf.format(new Date()));

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(userEmail.replace(".", "_"))
                .child(currentHistoryKey) // ✅ Dùng key từ startTime
                .child("endTime");

        dbRef.setValue(endTime)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian kết thúc đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian kết thúc", e));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean showBottomBar = prefs.getBoolean("showBottomBar", true);
    }

    // Ham tai truyen ve
    private void downloadPdfToAppFolder(String urlString, String fileName) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Tạo thư mục PDF riêng của app
                File pdfDir = new File(getExternalFilesDir(null), "PDF");
                if (!pdfDir.exists()) pdfDir.mkdirs();

                File pdfFile = new File(pdfDir, fileName);

                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[4096]; // tăng buffer cho nhanh
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.close();
                input.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Đã tải về: " + pdfFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Tải file thất bại!", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
