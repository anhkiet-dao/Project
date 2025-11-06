package com.example.do_an.application;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.do_an.R;
import com.example.do_an.Story.FavoriteManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

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

    private ImageView btnPrevPage, btnNextPage, btnZoomIn, btnZoomOut;

    // Các biến hỗ trợ
    private float currentZoom = 1.0f; // Mức zoom mặc định
    private int currentPage = 0;

    private String currentStoryId = "doraemon_001";
    private String currentTitle = "Doraemon";
    private String currentAuthor = "Fujiko F. Fujio";
    private String currentCategory = "Thiếu nhi";
    private String currentDescription = "Truyện tranh nổi tiếng của Nhật Bản";
    private String currentImageUrl = "https://example.com/doraemon.jpg";
    private String currentReadUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        txtTieuDe = findViewById(R.id.txtTieuDe);
        pdfViewPager = findViewById(R.id.pdfViewPager);
        btnFavorite = findViewById(R.id.btnFavorite);

        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);

        db = FirebaseFirestore.getInstance();
        favoriteManager = new FavoriteManager();

        // ✅ Lấy email người dùng đang đăng nhập
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
            Log.d(TAG, "Đăng nhập bằng email: " + userEmail);
        } else {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        txtTieuDe.setText(currentTitle);

        // 🔍 Kiểm tra xem truyện này đã được yêu thích chưa
        checkIfFavorite();

        // ❤️ Khi nhấn vào biểu tượng yêu thích
        btnFavorite.setOnClickListener(v -> toggleFavorite());

        // 📘 Tải PDF truyện
        loadPdfFromFirestore(currentTitle);

        // 🔹 Nút trang trước
        btnPrevPage.setOnClickListener(v -> {
            if (pdfPageAdapter == null) return;
            if (currentPage > 0) {
                currentPage--;
                pdfViewPager.setCurrentItem(currentPage, true);
            } else {
                Toast.makeText(this, "Đây là trang đầu tiên!", Toast.LENGTH_SHORT).show();
            }
        });
        // 🔹 Nút trang sau
        btnNextPage.setOnClickListener(v -> {
            if (pdfPageAdapter == null) return;
            int totalPages = pdfPageAdapter.getItemCount();
            if (currentPage < totalPages - 1) {
                currentPage++;
                pdfViewPager.setCurrentItem(currentPage, true);
            } else {
                Toast.makeText(this, "Đây là trang cuối cùng!", Toast.LENGTH_SHORT).show();
            }
        });
        // 🔹 Theo dõi trang hiện tại khi vuốt
        pdfViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
            }
        });
        // 🔹 Nút phóng to
        btnZoomIn.setOnClickListener(v -> {
            if (pdfViewPager.getScaleX() < 3.0f) {
                currentZoom += 0.25f;
                pdfViewPager.setScaleX(currentZoom);
                pdfViewPager.setScaleY(currentZoom);
            } else {
                Toast.makeText(this, "Đã phóng to tối đa!", Toast.LENGTH_SHORT).show();
            }
        });
        // 🔹 Nút thu nhỏ
        btnZoomOut.setOnClickListener(v -> {
            if (pdfViewPager.getScaleX() > 0.5f) {
                currentZoom -= 0.25f;
                pdfViewPager.setScaleX(currentZoom);
                pdfViewPager.setScaleY(currentZoom);
            } else {
                Toast.makeText(this, "Đã thu nhỏ tối đa!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 🔍 Kiểm tra truyện có trong danh sách yêu thích hay không
     */
    private void checkIfFavorite() {
        favoriteManager.getFavorites(userEmail, favorites -> {
            for (Map<String, Object> item : favorites) {
                if (item != null && currentStoryId.equals(item.get("storyId"))) {
                    isFavorite = true;
                    btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                    return;
                }
            }
            isFavorite = false;
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
        });
    }

    /**
     * ❤️ Toggle yêu thích
     */
    private void toggleFavorite() {
        if (!isFavorite) {
            // ➕ Thêm truyện yêu thích
            favoriteManager.addFavorite(
                    userEmail,
                    currentStoryId,
                    currentTitle,
                    currentAuthor,
                    currentCategory,
                    currentDescription,
                    currentImageUrl,
                    currentReadUrl
            );
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            Toast.makeText(this, "Đã thêm vào yêu thích ❤️", Toast.LENGTH_SHORT).show();
            isFavorite = true;
        } else {
            // ❌ Xóa truyện yêu thích
            favoriteManager.removeFavorite(userEmail, currentStoryId);
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            Toast.makeText(this, "Đã xóa khỏi yêu thích 💔", Toast.LENGTH_SHORT).show();
            isFavorite = false;
        }
    }

    private void loadPdfFromFirestore(String tenTruyen) {
        db.collection("doraemon")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String title = doc.getString("title");
                        if (title != null && title.equalsIgnoreCase(tenTruyen.trim())) {
                            String pdfUrl = doc.getString("pdfUrl");
                            if (pdfUrl != null && !pdfUrl.isEmpty()) {
                                currentReadUrl = pdfUrl;
                                downloadPdfToCache(pdfUrl, "temp_story.pdf");
                            } else {
                                Toast.makeText(this, "Không tìm thấy file PDF!", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                    }
                    Toast.makeText(this, "Không tìm thấy truyện '" + tenTruyen + "' trong Firestore!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void downloadPdfToCache(String pdfUrl, String fileName) {
        Toast.makeText(this, "Đang tải truyện...", Toast.LENGTH_SHORT).show();

        final String downloadUrl;
        if (pdfUrl.contains("drive.google.com/file/d/")) {
            String fileId = pdfUrl.split("/d/")[1].split("/")[0];
            downloadUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        } else {
            downloadUrl = pdfUrl;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Bắt đầu tải từ: " + downloadUrl);
                URL url = new URL(downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();
                File file = new File(getCacheDir(), fileName);
                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();

                Log.d(TAG, "Tải file thành công: " + file.getAbsolutePath());

                runOnUiThread(() -> setupPdfRenderer(file));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ReadActivity.this, "Tải PDF thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
        try {
            if (pdfPageAdapter != null) {
                pdfPageAdapter.close();
                Log.d(TAG, "Đã đóng PdfRenderer.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
