package com.example.do_an.UI;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.Series.SeriesFragment;
import com.example.do_an.application.Encryption;
import com.example.do_an.home.AllBooksFragment;
import com.example.do_an.home.Book;
import com.example.do_an.home.BookHomeAdapter;
import com.example.do_an.home.BookImageAdapter;
import com.example.do_an.home.PdfViewerUtility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {
    private CardView cardBook;
    private ImageView roundedImage;
    private final List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;
    private final Handler handler = new Handler();
    private final long DELAY_TIME = 3000; // thoi gian chuyen anh
    private final Runnable imageSwitcherRunnable = new Runnable() {
        @Override
        public void run() {
            if (!imageUrls.isEmpty()) {
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size();
                loadImageToImageView(imageUrls.get(currentImageIndex));
            }
            handler.postDelayed(this, DELAY_TIME);
        }
    };
    private RecyclerView rvPreview, rvNewBooks, rvPopularBooks, rvTrendBooks;
    private ViewPager2 pdfViewPager;
    private TextView txtPdfName, txtPdfAuthor, btnDetail;
    private View pdfInfoContainer;
    private Book currentViewingBook;
    private PdfViewerUtility pdfViewerUtility;
    private final List<Book> listPreview = new ArrayList<>();
    private final List<Book> listNew = new ArrayList<>();
    private final List<Book> listPopular = new ArrayList<>();
    private final List<Book> listTrend = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvGreeting;
    private TextView detailPreview;
    private TextView detailNewBooks, detailPopularBooks, detailTrendBooks;
    private BookImageAdapter previewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.home_fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mapping(view);
        setupRecycler();
        loadAllData();
        setupUserGreeting();
        setupDetailClickListeners();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopImageSwitcher();
        if (pdfViewerUtility != null) {
            pdfViewerUtility.closeRenderer();
        }
    }

    private void mapping(View view) {
        cardBook = view.findViewById(R.id.card_book);
        roundedImage = view.findViewById(R.id.rounded_image);

        rvPreview = view.findViewById(R.id.rv_books);
        rvNewBooks = view.findViewById(R.id.rv_books1);
        rvPopularBooks = view.findViewById(R.id.rv_books2);
        rvTrendBooks = view.findViewById(R.id.rv_books3);

        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        txtPdfName = view.findViewById(R.id.txtPdfName);
        txtPdfAuthor = view.findViewById(R.id.txtPdfAuthor);
        btnDetail = view.findViewById(R.id.btnDetail);

        tvGreeting = view.findViewById(R.id.txtGreeting);

        if (txtPdfName != null && txtPdfName.getParent() != null) {
            pdfInfoContainer = (View) txtPdfName.getParent().getParent();
        }

        pdfViewerUtility = new PdfViewerUtility(getContext(), pdfViewPager);

        detailPreview = view.findViewById(R.id.detail);

        detailNewBooks = view.findViewById(R.id.detail1);
        detailPopularBooks = view.findViewById(R.id.detail2);
        detailTrendBooks = view.findViewById(R.id.detail3);
    }

    private void setupRecycler() {
        rvPreview.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvNewBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvPopularBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvTrendBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
    }

    private void loadAllData() {
        loadCategory("reviewBooks", listPreview, rvPreview, true);

        // Giữ nguyên các danh mục khác
        loadCategory("newBooks", listNew, rvNewBooks, false);
        loadCategory("popularBooks", listPopular, rvPopularBooks, false);
        loadCategory("trendBooks", listTrend, rvTrendBooks, false);
    }

    private void setupDetailClickListeners() {
        if (detailNewBooks != null) {
            detailNewBooks.setOnClickListener(v -> navigateToAllBooksFragment("newBooks", "Truyện mới"));
        }

        if (detailPopularBooks != null) {
            detailPopularBooks.setOnClickListener(v -> navigateToAllBooksFragment("popularBooks", "Truyện phổ biến"));
        }

        if (detailTrendBooks != null) {
            detailTrendBooks.setOnClickListener(v -> navigateToAllBooksFragment("trendBooks", "Xu hướng đọc"));
        }
    }

    private void navigateToAllBooksFragment(String collectionName, String title) {
        if (getActivity() == null) return;

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

        AllBooksFragment allBooksFragment = new AllBooksFragment();
        Bundle args = new Bundle();
        args.putString("COLLECTION_NAME", collectionName);
        args.putString("TITLE", title);
        allBooksFragment.setArguments(args);

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.hide(this);

        transaction.add(R.id.fragment_container, allBooksFragment, "AllBooksFragment");
        transaction.addToBackStack(null);

        transaction.commit();
    }

    private void loadImageToImageView(String url) {
        if (roundedImage != null && url != null && !url.isEmpty() && getContext() != null) {
            int targetWidth = roundedImage.getWidth();
            if (targetWidth <= 0) targetWidth = dpToPx(400);
            int targetHeight = dpToPx(300);

            Glide.with(getContext())
                    .load(url)
                    .override(targetWidth, targetHeight)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(roundedImage);
        }
    }

    private void startImageSwitcher() {
        if (imageUrls.size() > 1) {
            currentImageIndex = 0;
            loadImageToImageView(imageUrls.get(currentImageIndex));
            handler.postDelayed(imageSwitcherRunnable, DELAY_TIME);
        } else if (!imageUrls.isEmpty()) {
            loadImageToImageView(imageUrls.get(0));
        }
    }

    private void stopImageSwitcher() {
        handler.removeCallbacks(imageSwitcherRunnable);
    }

    private void loadCategory(String collectionName, List<Book> list, RecyclerView recyclerView, boolean onlyImage) {
        db.collection(collectionName).get().addOnSuccessListener(snap -> {
            list.clear();

            boolean isPreviewList = (list == listPreview);

            if (isPreviewList) {
                stopImageSwitcher();
                imageUrls.clear();
            }

            for (DocumentSnapshot d : snap.getDocuments()) {
                Book b = d.toObject(Book.class);
                if (b != null) {
                    b.setId(d.getId());
                    list.add(b);
                    if (isPreviewList && b.getImageUrl() != null && !b.getImageUrl().isEmpty()) {
                        imageUrls.add(b.getImageUrl());
                    }
                }
            }

            if (isPreviewList) {
                startImageSwitcher();
                if (!list.isEmpty()) {
                    currentViewingBook = list.get(0);
                    showPdfPreview(currentViewingBook);

                    previewAdapter = new BookImageAdapter(getContext(), list, this::onBookClick);
                    recyclerView.setAdapter(previewAdapter);

                    previewAdapter.setSelectedBookId(currentViewingBook.getId());
                    previewAdapter.notifyDataSetChanged();


                    if (detailPreview != null) {
                        detailPreview.setOnClickListener(v -> navigateToAllBooksFragment("reviewBooks", "Truyện xem trước"));
                    }

                    if (btnDetail != null) {
                        btnDetail.setOnClickListener(v -> openReadFragmentDirectly(currentViewingBook));
                    }
                }
            }

            if (list.isEmpty()) {
                Toast.makeText(getContext(), collectionName + " trống hoặc lỗi.", Toast.LENGTH_SHORT).show();
                recyclerView.setAdapter(null);
            } else {
                if (!isPreviewList) {
                    // Giữ nguyên onBookClickOpenReadFragment (chuyển qua màn hình chi tiết Series) cho các danh sách khác
                    if (onlyImage) {
                        recyclerView.setAdapter(new BookImageAdapter(getContext(), list, this::onBookClick));
                    } else {
                        recyclerView.setAdapter(new BookHomeAdapter(getContext(), list, this::onBookClickOpenReadFragment));
                    }
                }
            }

            recyclerView.setVisibility(View.VISIBLE);

        }).addOnFailureListener(e -> {
            Log.e("HomeFragment", "Lỗi tải " + collectionName + ": " + e.getMessage());
            Toast.makeText(getContext(), "Lỗi mạng!", Toast.LENGTH_LONG).show();
            recyclerView.setVisibility(View.GONE);
        });
    }

    private void onBookClickOpenReadFragment(Book book) {
        if (getActivity() == null || book == null) return;

        SeriesFragment seriesFragment = new SeriesFragment();
        Bundle args = new Bundle();
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_NAME", book.getName() != null ? book.getName() : "Không rõ tên");
        args.putString("STORY_AUTHOR", book.getAuthor() != null ? book.getAuthor() : "Không rõ tác giả");
        args.putString("STORY_CATEGORY", book.getCategory() != null ? book.getCategory() : "Khác");
        args.putString("STORY_IMAGE_URL", book.getImageUrl() != null ? book.getImageUrl() : "");
        seriesFragment.setArguments(args);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, seriesFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openReadFragmentDirectly(Book book) {
        if (getActivity() == null || book == null) return;
        if (book.getLink() == null || book.getLink().isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy link đọc!", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("PDF_LINK", book.getLink()); // Dùng link trực tiếp
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_TITLE", book.getName() != null ? book.getName() : "Truyện");
        args.putString("TAP_TITLE", book.getName() != null ? book.getName() : "Tập 1"); // Tạm coi là tiêu đề tập
        args.putString("STORY_AUTHOR", book.getAuthor() != null ? book.getAuthor() : "Không rõ tác giả");
        args.putString("STORY_CATEGORY", book.getCategory() != null ? book.getCategory() : "Khác");
        args.putString("STORY_IMAGE_URL", book.getImageUrl() != null ? book.getImageUrl() : "");

        ReadFragment readFragment = ReadFragment.newInstance(args);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, readFragment)
                .addToBackStack(null)
                .commit();
    }


    private void onBookClick(Book book) {
        if (getActivity() == null || book == null) return;
        if (book.getLink() == null || book.getLink().isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy link đọc!", Toast.LENGTH_SHORT).show();
            return;
        }

        currentViewingBook = book;
        showPdfPreview(book);

        if (previewAdapter != null) {
            previewAdapter.setSelectedBookId(book.getId());
            previewAdapter.notifyDataSetChanged();
        }

        // 🌟 CHỈNH SỬA Ở ĐÂY: Dùng openReadFragmentDirectly cho btnDetail sau khi click vào sách preview
        if (btnDetail != null) {
            btnDetail.setOnClickListener(v -> openReadFragmentDirectly(book));
        }
    }

    private void showPdfPreview(Book book) {
        if (pdfViewerUtility == null || getContext() == null || pdfViewPager == null || book == null) return;
        pdfViewPager.setAdapter(null);

        pdfViewPager.setVisibility(View.VISIBLE);
        if (pdfInfoContainer != null) pdfInfoContainer.setVisibility(View.VISIBLE);
        txtPdfName.setText(book.getName() != null ? book.getName() : "Không rõ tên");
        txtPdfAuthor.setText(book.getAuthor() != null ? "Tác giả: " + book.getAuthor() : "Tác giả: ???");
        pdfViewerUtility.loadPdfPreview(book, 5);
    }

    private void setupUserGreeting() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            tvGreeting.setText("Chào bạn!");
            return;
        }

        final String userEmail = currentUser.getEmail();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String encryptedEmail = userSnap.child("email").getValue(String.class);
                    if (encryptedEmail == null) continue;

                    try {
                        String emailDecrypted = Encryption.decrypt(encryptedEmail.trim());
                        if (userEmail.equals(emailDecrypted)) {
                            String encryptedName = userSnap.child("fullName").getValue(String.class);
                            String realName = "Bạn";
                            if (encryptedName != null && !encryptedName.isEmpty()) {
                                realName = Encryption.decrypt(encryptedName.trim());
                            }

                            Calendar calendar = Calendar.getInstance();
                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            String greeting;
                            if (hour < 11) greeting = "Chào buổi sáng, ";
                            else if (hour < 13) greeting = "Chào buổi trưa, ";
                            else if (hour < 18) greeting = "Chào buổi chiều, ";
                            else greeting = "Chào buổi tối, ";

                            tvGreeting.setText(greeting + realName + "!");
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                tvGreeting.setText("Chào bạn!");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvGreeting.setText("Chào bạn!");
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}