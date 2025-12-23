package com.example.do_an.presentation.library.home;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.Book;
import com.example.do_an.presentation.library.home.adapter.BookHomeAdapter;
import com.example.do_an.presentation.library.home.adapter.BookImageAdapter;
import com.example.do_an.presentation.library.series.SeriesFragment;
import com.example.do_an.presentation.reading.reader.ReadFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ImageView imageBanner;
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

    private RecyclerView recyclerPreview,
            recyclerNewBooks,
            recyclerPopularBooks,
            recyclerTrendBooks;
    private ViewPager2 pdfViewPager;
    private TextView textPdfName, textPdfAuthor, btnDetail;
    private View pdfInfoContainer;
    private TextView textGreeting;
    private TextView textTitlePreview,
            textTitleNewBooks,
            textTitlePopularBooks,
            textTitleTrendBooks;
    private TextView textDetailPreview,
            textDetailNewBooks,
            textDetailPopularBooks,
            textDetailTrendBooks;

    private FirebaseFirestore firestore;
    private PdfViewerUtility pdfViewerUtility;
    private Book currentViewingBook;
    private BookImageAdapter previewAdapter;

    private final List<Book> listPreview = new ArrayList<>();
    private final List<Book> listNew = new ArrayList<>();
    private final List<Book> listPopular = new ArrayList<>();
    private final List<Book> listTrend = new ArrayList<>();

    private boolean isViewCreated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);
        firestore = FirebaseFirestore.getInstance();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        setupViews(view);
        loadViews();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
        bannerHandler.removeCallbacksAndMessages(null);
        if (pdfViewerUtility != null)
            pdfViewerUtility.closeRenderer();
    }

    private void setupViews(View view) {
        imageBanner = view.findViewById(R.id.imageBanner);

        recyclerPreview = view.findViewById(R.id.recyclerPreview);
        recyclerNewBooks = view.findViewById(R.id.recyclerNewBooks);
        recyclerPopularBooks = view.findViewById(R.id.recyclerPopularBooks);
        recyclerTrendBooks = view.findViewById(R.id.recyclerTrendBooks);

        pdfViewPager = view.findViewById(R.id.pdfViewPager);
        textPdfName = view.findViewById(R.id.textPdfName);
        textPdfAuthor = view.findViewById(R.id.textPdfAuthor);
        btnDetail = view.findViewById(R.id.btnDetail);

        textTitlePreview = view.findViewById(R.id.textTitlePreview);
        textTitleNewBooks = view.findViewById(R.id.textTitleNewBooks);
        textTitlePopularBooks = view.findViewById(R.id.textTitlePopularBooks);
        textTitleTrendBooks = view.findViewById(R.id.textTitleTrendBooks);

        textGreeting = view.findViewById(R.id.textGreeting);

        if (textPdfName != null && textPdfName.getParent() != null) {
            pdfInfoContainer = (View) textPdfName.getParent().getParent();
        }

        pdfViewerUtility = new PdfViewerUtility(getContext(), pdfViewPager);

        textDetailPreview = view.findViewById(R.id.textDetailPreview);
        textDetailNewBooks = view.findViewById(R.id.textDetailNewBooks);
        textDetailPopularBooks = view.findViewById(R.id.textDetailPopularBooks);
        textDetailTrendBooks = view.findViewById(R.id.textDetailTrendBooks);
    }

    private void loadViews() {
        setupRecyclerViews();
        fetchHomeBanner();
        fetchAllBookCategories();
        displayUserGreeting();
        refreshCategoryTitles();
    }

    private void setupListeners() {
        setupCategoryDetailListeners();
    }

    private void refreshCategoryTitles() {
        textTitlePreview.setText(getString(R.string.review_books));
        textTitleNewBooks.setText(getString(R.string.new_books));
        textTitlePopularBooks.setText(getString(R.string.popular_books));
        textTitleTrendBooks.setText(getString(R.string.trend_books));

        String detailText = getString(R.string.detail_text);
        textDetailPreview.setText(detailText);
        textDetailNewBooks.setText(detailText);
        textDetailPopularBooks.setText(detailText);
        textDetailTrendBooks.setText(detailText);
    }

    private void setupRecyclerViews() {
        recyclerPreview.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        recyclerNewBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        recyclerPopularBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
        recyclerTrendBooks.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false));
    }

    private void fetchHomeBanner() {
        firestore.collection("Banner").get()
                .addOnSuccessListener(snapshot -> {
                    if (!isViewCreated)
                        return;
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
                })
                .addOnFailureListener(e -> {
                    if (!isViewCreated)
                        return;
                });
    }

    private void loadBannerImage(String url) {
        if (!isViewCreated || getContext() == null || imageBanner == null)
            return;

        imageBanner.post(() -> {
            if (!isViewCreated || getContext() == null)
                return;
            int width = imageBanner.getWidth();
            int height = imageBanner.getHeight();

            Glide.with(getContext())
                    .load(url)
                    .override(width, height)
                    .fitCenter()
                    .placeholder(R.drawable.bg_splash)
                    .error(R.drawable.bg_splash)
                    .into(imageBanner);
        });
    }

    private void fetchAllBookCategories() {
        fetchAndDisplayCategory("reviewBooks", listPreview, recyclerPreview, true);
        fetchAndDisplayCategory("newBooks", listNew, recyclerNewBooks, false);
        fetchAndDisplayCategory("popularBooks", listPopular, recyclerPopularBooks, false);
        fetchAndDisplayCategory("trendBooks", listTrend, recyclerTrendBooks, false);
    }

    private void fetchAndDisplayCategory(String collection,
            List<Book> list,
            RecyclerView recyclerView,
            boolean isPreview) {

        firestore.collection(collection).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isViewCreated || getContext() == null)
                        return;
                    list.clear();

                    for (DocumentSnapshot d : snapshot) {
                        Book b = d.toObject(Book.class);
                        if (b != null) {
                            b.setId(d.getId());
                            list.add(b);
                        }
                    }

                    if (list.isEmpty())
                        return;

                    if (isPreview) {
                        currentViewingBook = list.get(0);
                        // Only preload first 3 PDFs to save memory
                        int preloadCount = Math.min(3, list.size());
                        for (int i = 0; i < preloadCount; i++) {
                            pdfViewerUtility.preloadPdf(list.get(i));
                        }

                        previewAdapter = new BookImageAdapter(getContext(), list, this::onBookClick);
                        recyclerView.setAdapter(previewAdapter);
                        previewAdapter.setSelectedBookId(currentViewingBook.getId());

                        showPdfPreview(currentViewingBook);
                    } else {
                        recyclerView.setAdapter(
                                new BookHomeAdapter(getContext(), list, this::navigateToSeriesFragment));
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isViewCreated)
                        return;
                });
    }

    private void onBookClick(Book book) {
        currentViewingBook = book;
        showPdfPreview(book);
        previewAdapter.setSelectedBookId(book.getId());
        previewAdapter.notifyDataSetChanged();
    }

    private void showPdfPreview(Book book) {
        if (book.getLink() == null)
            return;

        pdfViewPager.setVisibility(View.VISIBLE);
        pdfInfoContainer.setVisibility(View.VISIBLE);

        textPdfName.setText(book.getName());
        textPdfAuthor.setText(getString(R.string.author, book.getAuthor()));

        pdfViewerUtility.loadPdfPreview(book, 5);
        btnDetail.setOnClickListener(v -> navigateToReadFragment(book));
    }

    private void navigateToSeriesFragment(Book book) {
        SeriesFragment fragment = new SeriesFragment();
        Bundle args = new Bundle();
        args.putString("STORY_ID", book.getId());
        args.putString("STORY_NAME", book.getName());
        args.putString("STORY_AUTHOR", book.getAuthor());
        args.putString("STORY_IMAGE_URL", book.getImageUrl());
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void navigateToReadFragment(Book book) {
        ReadFragment readFragment = ReadFragment.newInstance(new Bundle());
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, readFragment)
                .addToBackStack(null)
                .commit();
    }

    private void displayUserGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            textGreeting.setText(getString(R.string.hello_user));
            return;
        }

        textGreeting.setText(getString(R.string.hello_user));
    }

    private void setupCategoryDetailListeners() {
        textDetailPreview
                .setOnClickListener(v -> navigateToAllBooksFragment("reviewBooks", getString(R.string.review_books)));
        textDetailNewBooks
                .setOnClickListener(v -> navigateToAllBooksFragment("newBooks", getString(R.string.new_books)));
        textDetailPopularBooks
                .setOnClickListener(v -> navigateToAllBooksFragment("popularBooks", getString(R.string.popular_books)));
        textDetailTrendBooks
                .setOnClickListener(v -> navigateToAllBooksFragment("trendBooks", getString(R.string.trend_books)));
    }

    private void navigateToAllBooksFragment(String collection, String title) {
        AllBooksFragment fragment = new AllBooksFragment();
        Bundle args = new Bundle();
        args.putString("COLLECTION_NAME", collection);
        args.putString("TITLE", title);
        fragment.setArguments(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}
