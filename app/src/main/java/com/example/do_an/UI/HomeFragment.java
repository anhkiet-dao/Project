package com.example.do_an.UI;

import android.content.res.Configuration;
import android.content.res.Resources;
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
import java.util.Locale;

public class HomeFragment extends Fragment {

    private CardView cardBook;
    private ImageView roundedImage;
    private final List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;
    private final Handler handler = new Handler();
    private final long DELAY_TIME = 3000; // thời gian chuyển ảnh
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
    private TextView title, title1, title2, title3;
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
        title = view.findViewById(R.id.title);
        title1 = view.findViewById(R.id.title1);
        title2 = view.findViewById(R.id.title2);
        title3 = view.findViewById(R.id.title3);

        tvGreeting = view.findViewById(R.id.txtGreeting);

        if (txtPdfName != null && txtPdfName.getParent() != null) {
            pdfInfoContainer = (View) txtPdfName.getParent().getParent();
        }

        pdfViewerUtility = new PdfViewerUtility(getContext(), pdfViewPager);

        detailPreview = view.findViewById(R.id.detail);
        detailNewBooks = view.findViewById(R.id.detail1);
        detailPopularBooks = view.findViewById(R.id.detail2);
        detailTrendBooks = view.findViewById(R.id.detail3);

        if (title != null) title.setText(getString(R.string.review_books));
        if (title1 != null) title1.setText(getString(R.string.new_books));
        if (title2 != null) title2.setText(getString(R.string.popular_books));
        if (title3 != null) title3.setText(getString(R.string.trend_books));

        // Set detail text đa ngôn ngữ
        String detailText = getString(R.string.detail_text);
        if (detailPreview != null) detailPreview.setText(detailText);
        if (detailNewBooks != null) detailNewBooks.setText(detailText);
        if (detailPopularBooks != null) detailPopularBooks.setText(detailText);
        if (detailTrendBooks != null) detailTrendBooks.setText(detailText);
    }

    private void setupRecycler() {
        rvPreview.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvNewBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvPopularBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvTrendBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
    }

    private void loadAllData() {
        loadCategory("reviewBooks", listPreview, rvPreview, true);
        loadCategory("newBooks", listNew, rvNewBooks, false);
        loadCategory("popularBooks", listPopular, rvPopularBooks, false);
        loadCategory("trendBooks", listTrend, rvTrendBooks, false);
    }

    private void setupDetailClickListeners() {
        if (detailPreview != null) {
            detailPreview.setOnClickListener(v -> navigateToAllBooksFragment("reviewBooks", getString(R.string.review_books)));
        }
        if (detailNewBooks != null) {
            detailNewBooks.setOnClickListener(v -> navigateToAllBooksFragment("newBooks", getString(R.string.new_books)));
        }
        if (detailPopularBooks != null) {
            detailPopularBooks.setOnClickListener(v -> navigateToAllBooksFragment("popularBooks", getString(R.string.popular_books)));
        }
        if (detailTrendBooks != null) {
            detailTrendBooks.setOnClickListener(v -> navigateToAllBooksFragment("trendBooks", getString(R.string.trend_books)));
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

        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, allBooksFragment, "AllBooksFragment")
                .addToBackStack(null)
                .commit();
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

                    // Preload tất cả PDF
                    for (Book b : list) {
                        pdfViewerUtility.preloadPdf(b);
                    }

                    showPdfPreview(currentViewingBook);

                    previewAdapter = new BookImageAdapter(getContext(), list, this::onBookClick);
                    recyclerView.setAdapter(previewAdapter);
                    previewAdapter.setSelectedBookId(currentViewingBook.getId());
                    previewAdapter.notifyDataSetChanged();
                }
            } else {
                if (!list.isEmpty()) {
                    if (onlyImage) {
                        recyclerView.setAdapter(new BookImageAdapter(getContext(), list, this::onBookClick));
                    } else {
                        recyclerView.setAdapter(new BookHomeAdapter(getContext(), list, this::onBookClickOpenReadFragment));
                    }
                } else {
                    recyclerView.setAdapter(null);
                }
            }

            recyclerView.setVisibility(View.VISIBLE);

        }).addOnFailureListener(e -> {
            Log.e("HomeFragment", "Lỗi tải " + collectionName + ": " + e.getMessage());
            Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
            recyclerView.setVisibility(View.GONE);
        });
    }

    private void onBookClickOpenReadFragment(Book book) {
        if (getActivity() == null || book == null) return;

        SeriesFragment seriesFragment = new SeriesFragment();
        Bundle args = new Bundle();
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_NAME", book.getName() != null ? book.getName() : getString(R.string.unknown_name));
        args.putString("STORY_AUTHOR", book.getAuthor() != null ? book.getAuthor() : getString(R.string.unknown_author));
        args.putString("STORY_CATEGORY", book.getCategory() != null ? book.getCategory() : getString(R.string.unknown_category));
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
            Toast.makeText(getContext(), getString(R.string.pdf_link_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putString("PDF_LINK", book.getLink());
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_TITLE", book.getName() != null ? book.getName() : getString(R.string.default_story_title));
        args.putString("TAP_TITLE", book.getName() != null ? book.getName() : getString(R.string.default_tap_title));
        args.putString("STORY_AUTHOR", book.getAuthor() != null ? book.getAuthor() : getString(R.string.unknown_author));
        args.putString("STORY_CATEGORY", book.getCategory() != null ? book.getCategory() : getString(R.string.unknown_category));
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
            Toast.makeText(getContext(), getString(R.string.pdf_link_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        currentViewingBook = book;
        showPdfPreview(book);

        if (previewAdapter != null) {
            previewAdapter.setSelectedBookId(book.getId());
            previewAdapter.notifyDataSetChanged();
        }
    }

    private void showPdfPreview(Book book) {
        if (pdfViewerUtility == null || getContext() == null || pdfViewPager == null || book == null) return;

        if (book.getLink() == null || book.getLink().isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.pdf_link_not_found), Toast.LENGTH_SHORT).show();
            pdfViewPager.setVisibility(View.GONE);
            if (pdfInfoContainer != null) pdfInfoContainer.setVisibility(View.GONE);
            return;
        }

        pdfViewPager.setAdapter(null);
        pdfViewPager.setVisibility(View.VISIBLE);
        if (pdfInfoContainer != null) pdfInfoContainer.setVisibility(View.VISIBLE);
        txtPdfName.setText(book.getName() != null ? book.getName() : getString(R.string.unknown_name));
        txtPdfAuthor.setText(getString(R.string.author, book.getAuthor() != null ? book.getAuthor() : getString(R.string.unknown_author)));

        pdfViewerUtility.loadPdfPreview(book, 5);

        if (btnDetail != null) {
            btnDetail.setOnClickListener(v -> openReadFragmentDirectly(book));
        }
    }

    private void setupUserGreeting() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            tvGreeting.setText(getString(R.string.hello_user));
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
                            String realName = getString(R.string.default_user_name);
                            if (encryptedName != null && !encryptedName.isEmpty()) {
                                realName = Encryption.decrypt(encryptedName.trim());
                            }

                            Calendar calendar = Calendar.getInstance();
                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            String greeting;
                            if (hour < 11) greeting = getString(R.string.greeting_morning);
                            else if (hour < 13) greeting = getString(R.string.greeting_noon);
                            else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
                            else greeting = getString(R.string.greeting_evening);

                            tvGreeting.setText(greeting + ", " + realName + "!");
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                tvGreeting.setText(getString(R.string.hello_user));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvGreeting.setText(getString(R.string.hello_user));
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /** Hàm chuyển đổi ngôn ngữ runtime */
    public void switchLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Reload fragment để cập nhật text
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}
