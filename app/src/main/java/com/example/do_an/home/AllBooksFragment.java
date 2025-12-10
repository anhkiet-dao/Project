package com.example.do_an.home;

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
import com.example.do_an.Series.SeriesFragment;
import com.example.do_an.application.Encryption;
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
    private TextView tvGreeting; // Biến mới
    private FirebaseFirestore db;
    private final List<Book> bookList = new ArrayList<>();
    private AllBooksAdapter adapter;
    private String collectionName;
    private String title;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy danh mục.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void mapping(View view) {
        rvAllBooks = view.findViewById(R.id.rv_all_books);
        toolbarTitle = view.findViewById(R.id.toolbar_title);
        tvGreeting = view.findViewById(R.id.txtGreeting);
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
                Toast.makeText(getContext(), "Không tìm thấy cuốn sách nào trong danh mục này.", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();

        }).addOnFailureListener(e -> {
            Log.e("AllBooksFragment", "Lỗi tải sách: " + e.getMessage());
            Toast.makeText(getContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_LONG).show();
        });
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

    @Override
    public void onBookClick(Book book) {
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
}