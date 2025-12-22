package com.example.do_an.presentation.library.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.domain.library.model.Book;
import com.example.do_an.presentation.library.home.adapter.AllBooksAdapter;
import com.example.do_an.presentation.library.series.SeriesFragment;
import com.example.do_an.core.utils.Encryption;
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

public class AllBooksFragment extends Fragment implements AllBooksAdapter.BookClickListener {

    private RecyclerView rvAllBooks;
    private TextView toolbarTitle;
    private TextView tvGreeting;
    private FirebaseFirestore db;
    private final List<Book> bookList = new ArrayList<>();
    private AllBooksAdapter adapter;
    private String collectionName;
    private String title;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment_all_books, container, false);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            collectionName = getArguments().getString("COLLECTION_NAME");
            title = getArguments().getString("TITLE");
        }

        mapping(view);
        setupToolbar(view);
        setupRecyclerView();
        setupUserGreeting();

        if (collectionName != null) {
            loadBooks(collectionName);
        } else {
            Toast.makeText(getContext(), getString(R.string.error_no_category), Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void mapping(View view) {
        rvAllBooks = view.findViewById(R.id.rv_all_books);
        toolbarTitle = view.findViewById(R.id.toolbar_title);
        tvGreeting = view.findViewById(R.id.textGreeting);
    }

    private void setupToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (title != null) {
            toolbarTitle.setText(title);
        }

        toolbar.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupRecyclerView() {
        rvAllBooks.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new AllBooksAdapter(getContext(), bookList, this);
        rvAllBooks.setAdapter(adapter);
    }

    private void loadBooks(String collectionName) {
        db.collection(collectionName).get().addOnSuccessListener(snap -> {
            bookList.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Book b = d.toObject(Book.class);
                if (b != null) {
                    b.setId(d.getId());
                    bookList.add(b);
                }
            }

            if (bookList.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.no_books_found), Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Log.e("AllBooksFragment", "Error loading books: " + e.getMessage());
            Toast.makeText(getContext(), getString(R.string.network_error), Toast.LENGTH_LONG).show();
        });
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

    @Override
    public void onBookClick(Book book) {
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
                .add(R.id.fragmentContainer, seriesFragment)
                .addToBackStack(null)
                .commit();
    }

    /** Hàm chuyển đổi ngôn ngữ runtime */
    public void switchLanguage(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Resources resources = getResources();
        android.content.res.Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Reload fragment để cập nhật text
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}
