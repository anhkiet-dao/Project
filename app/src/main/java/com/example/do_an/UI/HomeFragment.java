package com.example.do_an.UI;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.Series.SeriesFragment;
import com.example.do_an.home.AllBooksFragment;
import com.example.do_an.home.Book;
import com.example.do_an.home.BookHomeAdapter;
import com.example.do_an.home.BookImageAdapter;
import com.example.do_an.home.PdfViewerUtility;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    // ================= BANNER =================
    private ImageView roundedImage;
    private final List<String> bannerUrls = new ArrayList<>();
    private int currentBannerIndex = 0;
    private final Handler bannerHandler = new Handler();
    private static final long BANNER_DELAY = 3000;

    private final Runnable bannerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!bannerUrls.isEmpty()) {
                currentBannerIndex = (currentBannerIndex + 1) % bannerUrls.size();
                loadBannerImage(bannerUrls.get(currentBannerIndex));
            }
            bannerHandler.postDelayed(this, BANNER_DELAY);
        }
    };

    // ================= VIEW =================
    private CardView cardBook;
    private RecyclerView rvPreview, rvNewBooks, rvPopularBooks, rvTrendBooks;
    private ViewPager2 pdfViewPager;
    private TextView txtPdfName, txtPdfAuthor, btnDetail;
    private View pdfInfoContainer;
    private TextView tvGreeting;
    private TextView title, title1, title2, title3;
    private TextView detailPreview, detailNewBooks, detailPopularBooks, detailTrendBooks;

    // ================= DATA =================
    private FirebaseFirestore db;
    private PdfViewerUtility pdfViewerUtility;
    private Book currentViewingBook;
    private BookImageAdapter previewAdapter;

    private final List<Book> listPreview = new ArrayList<>();
    private final List<Book> listNew = new ArrayList<>();
    private final List<Book> listPopular = new ArrayList<>();
    private final List<Book> listTrend = new ArrayList<>();

    // ================= LIFECYCLE =================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ui_fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mapping(view);
        setupRecycler();

        loadHomeBanner();     // ✅ Banner lấy từ collection Banner
        loadAllData();        // sách
        setupUserGreeting();
        setupDetailClickListeners();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bannerHandler.removeCallbacks(bannerRunnable);
        if (pdfViewerUtility != null) pdfViewerUtility.closeRenderer();
    }

    // ================= INIT =================

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

        title.setText(getString(R.string.review_books));
        title1.setText(getString(R.string.new_books));
        title2.setText(getString(R.string.popular_books));
        title3.setText(getString(R.string.trend_books));

        String detailText = getString(R.string.detail_text);
        detailPreview.setText(detailText);
        detailNewBooks.setText(detailText);
        detailPopularBooks.setText(detailText);
        detailTrendBooks.setText(detailText);
    }

    private void setupRecycler() {
        rvPreview.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvNewBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvPopularBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        rvTrendBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
    }

    // ================= BANNER =================

    private void loadHomeBanner() {
        db.collection("Banner").get().addOnSuccessListener(snapshot -> {
            bannerUrls.clear();
            bannerHandler.removeCallbacks(bannerRunnable);

            for (DocumentSnapshot d : snapshot) {
                String url = d.getString("imageUrl");
                if (url != null && !url.isEmpty()) {
                    bannerUrls.add(url);
                }
            }

            if (!bannerUrls.isEmpty()) {
                currentBannerIndex = 0;
                loadBannerImage(bannerUrls.get(0));
                bannerHandler.postDelayed(bannerRunnable, BANNER_DELAY);
            }
        });
    }

    private void loadBannerImage(String url) {
        if (getContext() == null || roundedImage == null) return;

        roundedImage.post(() -> {
            int width = roundedImage.getWidth();
            int height = roundedImage.getHeight();

            Glide.with(getContext())
                    .load(url)
                    .override(width, height)
                    .fitCenter()              // ✅ KHÔNG CẮT ẢNH
                    .placeholder(R.drawable.bg_splash)
                    .error(R.drawable.bg_splash)
                    .into(roundedImage);
        });
    }



    // ================= BOOKS =================

    private void loadAllData() {
        loadCategory("reviewBooks", listPreview, rvPreview, true);
        loadCategory("newBooks", listNew, rvNewBooks, false);
        loadCategory("popularBooks", listPopular, rvPopularBooks, false);
        loadCategory("trendBooks", listTrend, rvTrendBooks, false);
    }

    private void loadCategory(String collection,
                              List<Book> list,
                              RecyclerView recyclerView,
                              boolean isPreview) {

        db.collection(collection).get().addOnSuccessListener(snapshot -> {
            list.clear();

            for (DocumentSnapshot d : snapshot) {
                Book b = d.toObject(Book.class);
                if (b != null) {
                    b.setId(d.getId());
                    list.add(b);
                }
            }

            if (list.isEmpty()) return;

            if (isPreview) {
                currentViewingBook = list.get(0);
                for (Book b : list) pdfViewerUtility.preloadPdf(b);

                previewAdapter = new BookImageAdapter(getContext(), list, this::onBookClick);
                recyclerView.setAdapter(previewAdapter);
                previewAdapter.setSelectedBookId(currentViewingBook.getId());

                showPdfPreview(currentViewingBook);
            } else {
                recyclerView.setAdapter(
                        new BookHomeAdapter(getContext(), list, this::onBookClickOpenReadFragment)
                );
            }
        });
    }

    private void onBookClick(Book book) {
        currentViewingBook = book;
        showPdfPreview(book);
        previewAdapter.setSelectedBookId(book.getId());
        previewAdapter.notifyDataSetChanged();
    }

    private void showPdfPreview(Book book) {
        if (book.getLink() == null) return;

        pdfViewPager.setVisibility(View.VISIBLE);
        pdfInfoContainer.setVisibility(View.VISIBLE);

        txtPdfName.setText(book.getName());
        txtPdfAuthor.setText(getString(R.string.author, book.getAuthor()));

        pdfViewerUtility.loadPdfPreview(book, 5);
        btnDetail.setOnClickListener(v -> openReadFragmentDirectly(book));
    }

    private void onBookClickOpenReadFragment(Book book) {
        SeriesFragment fragment = new SeriesFragment();
        Bundle args = new Bundle();
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_NAME", book.getName());
        args.putString("STORY_AUTHOR", book.getAuthor());
        args.putString("STORY_IMAGE_URL", book.getImageUrl());
        fragment.setArguments(args);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openReadFragmentDirectly(Book book) {
        ReadFragment readFragment = ReadFragment.newInstance(new Bundle());
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, readFragment)
                .addToBackStack(null)
                .commit();
    }

    // ================= USER =================

    private void setupUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvGreeting.setText(getString(R.string.hello_user));
            return;
        }

        tvGreeting.setText(getString(R.string.hello_user));
    }

    // ================= DETAIL =================

    private void setupDetailClickListeners() {
        detailPreview.setOnClickListener(v ->
                navigateToAllBooksFragment("reviewBooks", getString(R.string.review_books)));
        detailNewBooks.setOnClickListener(v ->
                navigateToAllBooksFragment("newBooks", getString(R.string.new_books)));
        detailPopularBooks.setOnClickListener(v ->
                navigateToAllBooksFragment("popularBooks", getString(R.string.popular_books)));
        detailTrendBooks.setOnClickListener(v ->
                navigateToAllBooksFragment("trendBooks", getString(R.string.trend_books)));
    }

    private void navigateToAllBooksFragment(String collection, String title) {
        AllBooksFragment fragment = new AllBooksFragment();
        Bundle args = new Bundle();
        args.putString("COLLECTION_NAME", collection);
        args.putString("TITLE", title);
        fragment.setArguments(args);

        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ================= LANGUAGE =================

    public void switchLanguage(String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Resources res = getResources();
        Configuration config = res.getConfiguration();
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
        requireFragmentManager().beginTransaction().detach(this).attach(this).commit();
    }
}
