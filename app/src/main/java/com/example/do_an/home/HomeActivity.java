//package com.example.do_an.home;
//
//// ĐÃ XÓA DÒNG LỖI: import static androidx.core.util.TypedValueCompat.dpToPx;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.cardview.widget.CardView;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide; // Cần import Glide
//import com.example.do_an.R;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class HomeActivity extends AppCompatActivity {
//
//    private androidx.appcompat.widget.SearchView searchView;
//
//    // KHAI BÁO MỚI CHO SLIDESHOW
//    private CardView cardBook;
//    private ImageView roundedImage;
//    private final List<String> imageUrls = new ArrayList<>(); // Danh sách URL ảnh cho Slideshow
//    private int currentImageIndex = 0;
//    private final android.os.Handler handler = new android.os.Handler();
//    private final long DELAY_TIME = 3000; // Thời gian chuyển đổi ảnh (3 giây)
//
//    // Runnable chứa logic chuyển đổi ảnh
//    private final Runnable imageSwitcherRunnable = new Runnable() {
//        @Override
//        public void run() {
//            if (!imageUrls.isEmpty()) {
//                // Tính toán chỉ số ảnh tiếp theo (quay lại 0 nếu đã hết)
//                currentImageIndex = (currentImageIndex + 1) % imageUrls.size();
//                String imageUrl = imageUrls.get(currentImageIndex);
//
//                // Tải và hiển thị ảnh
//                loadImageToImageView(imageUrl);
//            }
//            // Lặp lại Runnable sau DELAY_TIME
//            handler.postDelayed(this, DELAY_TIME);
//        }
//    };
//    // KẾT THÚC KHAI BÁO MỚI
//
//    // KHAI BÁO 4 RECYCLERVIEW
//    private RecyclerView rvPreview; // Dành cho "Xem truyện trước"
//    private RecyclerView rvNewBooks, rvPopularBooks, rvTrendBooks;
//
//    // KHAI BÁO 4 LIST DỮ LIỆU
//    private final List<Book> listPreview = new ArrayList<>();
//    private final List<Book> listNew = new ArrayList<>();
//    private final List<Book> listPopular = new ArrayList<>();
//    private final List<Book> listTrend = new ArrayList<>();
//
//    private FirebaseFirestore db;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.home_fragment_home);
//
//        db = FirebaseFirestore.getInstance();
//
//        mapping();
//        setupRecycler();
//        loadAllData();
//    }
//
//    // --- QUẢN LÝ VÒNG ĐỜI (LIFECYCLE) ---
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // RẤT QUAN TRỌNG: Dừng Slideshow khi Activity bị hủy để tránh rò rỉ bộ nhớ
//        stopImageSwitcher();
//    }
//    // ----------------------------------------
//
//    private void mapping() {
//        searchView = findViewById(R.id.search_view);
//
//        // ÁNH XẠ MỚI CHO SLIDESHOW
//        cardBook = findViewById(R.id.card_book);
//        roundedImage = findViewById(R.id.rounded_image);
//
//        // ÁNH XẠ CHO "XEM TRUYỆN TRƯỚC" (ID: rv_books)
//        rvPreview = findViewById(R.id.rv_books);
//
//        rvNewBooks = findViewById(R.id.rv_books1);
//        rvPopularBooks = findViewById(R.id.rv_books2);
//        rvTrendBooks = findViewById(R.id.rv_books3);
//    }
//
//    private void setupRecycler() {
//        // SETUP LAYOUT MANAGER CHO "XEM TRUYỆN TRƯỚC"
//        if (rvPreview != null) {
//            rvPreview.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
//        }
//
//        if (rvNewBooks != null) {
//            rvNewBooks.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
//        }
//        if (rvPopularBooks != null) {
//            rvPopularBooks.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
//        }
//        if (rvTrendBooks != null) {
//            rvTrendBooks.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
//        }
//    }
//
//    private void loadAllData() {
//        // TẢI DỮ LIỆU CHO "XEM TRUYỆN TRƯỚC" (collection 'newBooks')
//        // Dữ liệu này vừa dùng cho rvPreview, vừa dùng để lấy URL cho Slideshow
//        loadCategory("newBooks", listPreview, rvPreview, true);
//
//        loadCategory("newBooks", listNew, rvNewBooks, true);
//        loadCategory("popularBooks", listPopular, rvPopularBooks, false);
//        loadCategory("trendBooks", listTrend, rvTrendBooks, false);
//    }
//
//    // --- PHƯƠNG THỨC XỬ LÝ SLIDESHOW ---
//
//    private void loadImageToImageView(String url) {
//        if (roundedImage != null && url != null && !url.isEmpty()) {
//
//            // 1. Xác định Chiều Rộng (Dùng kích thước ImageView)
//            int targetWidth = roundedImage.getWidth();
//            if (targetWidth <= 0) {
//                // Trường hợp ImageView chưa được vẽ, dùng giá trị mặc định (400dp)
//                targetWidth = dpToPx(400);
//            }
//
//            // 2. Định nghĩa Chiều Cao Tối Đa hợp lý (Ví dụ: 250dp)
//            final int MAX_HEIGHT_DP = 300;
//            int targetHeight = dpToPx(MAX_HEIGHT_DP); // Chuyển 250dp sang pixel
//
//            // Sử dụng Glide để tải ảnh
//            Glide.with(this)
//                    .load(url)
//                    // Đặt kích thước tối đa cho Glide: chiều rộng tối đa, chiều cao tối đa 250dp
//                    .override(targetWidth, targetHeight)
//                    // Giữ nguyên centerCrop (vì bạn muốn nó)
//                    .centerCrop()
//                    .placeholder(R.drawable.ic_launcher_background)
//                    .error(R.drawable.ic_launcher_background)
//                    .into(roundedImage);
//        }
//    }
//
//    private void startImageSwitcher() {
//        if (imageUrls.size() > 1) {
//            currentImageIndex = 0; // Bắt đầu từ ảnh đầu tiên
//            // Hiển thị ảnh đầu tiên ngay lập tức
//            loadImageToImageView(imageUrls.get(currentImageIndex));
//            // Bắt đầu lặp lại việc chuyển đổi ảnh
//            handler.postDelayed(imageSwitcherRunnable, DELAY_TIME);
//        } else if (!imageUrls.isEmpty()) {
//            // Chỉ có 1 ảnh, hiển thị và không chạy Slideshow
//            loadImageToImageView(imageUrls.get(0));
//        }
//    }
//
//    private void stopImageSwitcher() {
//        handler.removeCallbacks(imageSwitcherRunnable);
//    }
//
//    // ----------------------------------------
//
//    // Phương thức tải dữ liệu chung (Đã sửa lỗi ẩn RecyclerView và giảm lọc dữ liệu)
//    private void loadCategory(String collectionName, List<Book> list, RecyclerView recyclerView, boolean onlyImage) {
//        if (recyclerView == null) {
//            Log.e("HomeActivity", "RecyclerView là null cho collection: " + collectionName);
//            return;
//        }
//
//        db.collection(collectionName).get().addOnSuccessListener(snap -> {
//            list.clear();
//
//            // Xử lý Slideshow: chỉ áp dụng cho listPreview
//            boolean isPreviewList = (collectionName.equals("newBooks") && list == listPreview);
//            if (isPreviewList) {
//                stopImageSwitcher();
//                imageUrls.clear(); // Xóa danh sách URL cũ
//            }
//
//            for (DocumentSnapshot d : snap.getDocuments()) {
//                // toObject() yêu cầu lớp Book có hàm khởi tạo không đối số!
//                Book b = d.toObject(Book.class);
//
//                // Giảm bớt kiểm tra lọc dữ liệu để hiển thị tối đa
//                if (b != null) {
//                    list.add(b);
//
//                    // Thêm URL ảnh vào danh sách Slideshow
//                    if (isPreviewList && b.getImageUrl() != null && !b.getImageUrl().isEmpty()) {
//                        imageUrls.add(b.getImageUrl());
//                    }
//                } else {
//                    // Cảnh báo nếu có lỗi ánh xạ
//                    Log.w("FirestoreData", "Lỗi ánh xạ Book trong " + collectionName + ".");
//                }
//            }
//
//            // Khởi động Slideshow sau khi tải xong listPreview
//            if (isPreviewList) {
//                startImageSwitcher();
//            }
//
//            if (list.isEmpty()) {
//                // Không ẩn RecyclerView nếu trống, chỉ thông báo để debug
//                Toast.makeText(this, collectionName + " trống hoặc dữ liệu bị lỗi.", Toast.LENGTH_SHORT).show();
//                recyclerView.setAdapter(null); // Set adapter rỗng
//            } else {
//                // Set Adapter đúng loại
//                if (onlyImage) {
//                    // Sử dụng BookImageAdapter (chỉ ảnh) cho Xem trước và Truyện mới
//                    recyclerView.setAdapter(new BookImageAdapter(this, list, this::onBookClick));
//                } else {
//                    // Sử dụng BookHomeAdapter (ảnh + tên) cho Phổ biến và Xu hướng
//                    recyclerView.setAdapter(new BookHomeAdapter(this, list, this::onBookClick));
//                }
//            }
//
//            // Luôn đặt VISIBLE để các tiêu đề không bị mất
//            recyclerView.setVisibility(View.VISIBLE);
//
//        }).addOnFailureListener(e -> {
//            Log.e("HomeActivity", "Lỗi tải dữ liệu " + collectionName + ": " + e.getMessage(), e);
//            Toast.makeText(this, "Lỗi tải dữ liệu " + collectionName + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
//            recyclerView.setVisibility(View.GONE); // Ẩn khi lỗi mạng thực sự
//        });
//    }
//
//    private void onBookClick(Book book) {
//        String bookName = book.getName();
//        Toast.makeText(this, "Bạn chọn: " + (bookName != null ? bookName : "Sách không tên"), Toast.LENGTH_SHORT).show();
//        // TODO: Mở activity xem truyện PDF
//    }
//
//    // PHƯƠNG THỨC TIỆN ÍCH CHUYỂN DP SANG PIXEL TỰ ĐỊNH NGHĨA
//    private int dpToPx(int dp) {
//        float density = getResources().getDisplayMetrics().density;
//        return Math.round(dp * density);
//    }
//}