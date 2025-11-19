package com.example.do_an.Story;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.application.ReadActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import androidx.recyclerview.widget.GridLayoutManager;

public class SeriesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SeriesAdapter adapter;
    private final List<Series> seriesList = new ArrayList<>();
    private FirebaseFirestore db;

    // 🔹 Thêm các biến để lưu trữ TOÀN BỘ thông tin truyện
    private String storyId, storyName, storyAuthor, storyCategory, storyDescription, storyImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_series);

        // 🔹 Nhận TOÀN BỘ thông tin truyện từ Intent
        // (Activity trước đó, ví dụ StoryDetailActivity, PHẢI gửi những thông tin này)
        storyId = getIntent().getStringExtra("STORY_ID");
        storyName = getIntent().getStringExtra("STORY_NAME"); // Hoặc "STORY_TITLE"
        storyAuthor = getIntent().getStringExtra("STORY_AUTHOR");
        storyCategory = getIntent().getStringExtra("STORY_CATEGORY");
        storyDescription = getIntent().getStringExtra("STORY_DESCRIPTION");
        storyImageUrl = getIntent().getStringExtra("STORY_IMAGE_URL");

        if (storyId == null || storyId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy truyện!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Hiển thị tên truyện chính (storyName) thay vì tên tập
        toolbar.setTitle(storyName != null && !storyName.isEmpty() ? storyName : "Danh sách tập");
        toolbar.setNavigationOnClickListener(v -> finish());

        // RecyclerView
        recyclerView = findViewById(R.id.recyclerSeries);
        int numberOfColumns = 3;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        // ⭐️ SỬA ĐỔI QUAN TRỌNG: Gửi cả thông tin truyện VÀ thông tin tập
        adapter = new SeriesAdapter(seriesList, series -> {
            Intent intent = new Intent(SeriesActivity.this, ReadActivity.class);

            // 1. Gửi thông tin của TRUYỆN CHÍNH (để xử lý Favorite, History)
            intent.putExtra("STORY_ID", storyId);
            intent.putExtra("STORY_TITLE", storyName); // Tên truyện chính
            intent.putExtra("STORY_AUTHOR", storyAuthor);
            intent.putExtra("STORY_CATEGORY", storyCategory);
            intent.putExtra("STORY_DESCRIPTION", storyDescription);
            intent.putExtra("STORY_IMAGE_URL", storyImageUrl);

            // 2. Gửi thông tin của TẬP ĐƯỢC CHỌN (để đọc)
            intent.putExtra("PDF_LINK", series.getLink()); // Link PDF của tập
            intent.putExtra("TAP", series.getName());    // Tên của tập (ví dụ "Tập 01")

            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadSeries();
    }

    private void loadSeries() {
        db.collection("ic_story.svg")
                .document(storyId)
                .collection("Series")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    seriesList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Series s = doc.toObject(Series.class);
                        s.setId(doc.getId());
                        if (s.getName() != null && s.getLink() != null) {
                            seriesList.add(s);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (seriesList.isEmpty()) {
                        Toast.makeText(this, "Chưa có tập nào!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show());
    }
}