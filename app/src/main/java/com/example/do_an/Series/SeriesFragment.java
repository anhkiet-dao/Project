package com.example.do_an.Series;

import android.os.Bundle;
import android.util.Log; // Thêm import Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.UI.ReadFragment;
// Thêm các thư viện cần thiết để lấy thông tin người dùng
import com.example.do_an.application.Encryption; // Cần có lớp Encryption
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar; // Cần import Calendar
import java.util.List;

public class SeriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvNoSeries;
    private TextView txtGreeting;
    private SeriesAdapter adapter;
    private final List<Series> seriesList = new ArrayList<>();
    private FirebaseFirestore db;
    private String storyId, storyName, storyAuthor, storyCategory, storyDescription, storyImageUrl;

    public SeriesFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.series_activity_series, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtGreeting = view.findViewById(R.id.txtGreeting);

        setupUserGreeting();

        Bundle args = getArguments();
        if (args != null) {
            storyId = args.getString("STORY_ID");
            storyName = args.getString("STORY_NAME");
            storyAuthor = args.getString("STORY_AUTHOR");
            storyCategory = args.getString("STORY_CATEGORY");
            storyDescription = args.getString("STORY_DESCRIPTION");
            storyImageUrl = args.getString("STORY_IMAGE_URL");
        }

        if (storyId == null || storyId.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Không tìm thấy truyện!", Toast.LENGTH_SHORT).show();
            }
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(storyName != null && !storyName.isEmpty() ? storyName : "Danh sách tập");
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        recyclerView = view.findViewById(R.id.recyclerSeries);
        tvNoSeries = view.findViewById(R.id.tvNoSeries);
        int numberOfColumns = 3;

        if (getContext() != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));

            adapter = new SeriesAdapter(seriesList, series -> {
                if (getContext() == null || getActivity() == null) return;

                Bundle readArgs = new Bundle();

                readArgs.putString("STORY_ID", storyId);
                readArgs.putString("STORY_TITLE", storyName);
                readArgs.putString("STORY_AUTHOR", storyAuthor);
                readArgs.putString("STORY_CATEGORY", storyCategory);
                readArgs.putString("STORY_DESCRIPTION", storyDescription);
                readArgs.putString("STORY_IMAGE_URL", storyImageUrl);

                readArgs.putString("PDF_LINK", series.getLink());
                readArgs.putString("TAP", series.getName());

                ReadFragment readFragment = new ReadFragment();
                readFragment.setArguments(readArgs);

                getActivity().getSupportFragmentManager()
                        .beginTransaction()

                        .add(R.id.fragment_container, readFragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        }

        db = FirebaseFirestore.getInstance();
        loadSeries();
    }

    private void setupUserGreeting() {
        if (txtGreeting == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            txtGreeting.setText("Chào bạn!");
            return;
        }

        final String userEmail = currentUser.getEmail();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                String defaultName = "Bạn";
                String realName = defaultName;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String encryptedEmail = userSnap.child("email").getValue(String.class);
                    if (encryptedEmail == null) continue;

                    try {
                        String emailDecrypted = Encryption.decrypt(encryptedEmail.trim());
                        if (userEmail.equals(emailDecrypted)) {
                            String encryptedName = userSnap.child("fullName").getValue(String.class);
                            if (encryptedName != null && !encryptedName.isEmpty()) {
                                realName = Encryption.decrypt(encryptedName.trim());
                            }
                            break; // Đã tìm thấy tên, thoát vòng lặp
                        }
                    } catch (Exception e) {
                        Log.e("SeriesFragment", "Lỗi giải mã email/tên: " + e.getMessage());
                    }
                }

                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour < 11) greeting = "Chào buổi sáng, ";
                else if (hour < 13) greeting = "Chào buổi trưa, ";
                else if (hour < 18) greeting = "Chào buổi chiều, ";
                else greeting = "Chào buổi tối, ";

                txtGreeting.setText(greeting + realName + "!");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                Log.e("SeriesFragment", "Lỗi tải dữ liệu user: " + error.getMessage());
                txtGreeting.setText("Chào bạn!");
            }
        });
    }

    private void loadSeries() {
        if (storyId == null || db == null) return;

        db.collection("story")
                .document(storyId)
                .collection("Series")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Kiểm tra Context và Fragment có được gắn chưa
                    if (getContext() == null || !isAdded()) return;

                    seriesList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Series s = doc.toObject(Series.class);
                        s.setId(doc.getId());
                        if (s.getName() != null && s.getLink() != null) {
                            seriesList.add(s);
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    if (seriesList.isEmpty()) {
                        if (tvNoSeries != null) {
                            tvNoSeries.setText("Không có tập nào");
                            tvNoSeries.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    } else {
                        if (tvNoSeries != null) {
                            tvNoSeries.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        if (tvNoSeries != null && seriesList.isEmpty()) {
                            tvNoSeries.setText("Lỗi tải dữ liệu!");
                            tvNoSeries.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(getContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}