package com.example.do_an.Story;


import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.application.ReadActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;

public class ListActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    StoryAdapter storyAdapter;
    List<Story> storyList;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        recyclerView = findViewById(R.id.recyclerViewStories);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        storyList = new ArrayList<>();
        storyAdapter = new StoryAdapter(this, storyList);
        recyclerView.setAdapter(storyAdapter);

        storyAdapter.setOnStoryClickListener(story -> {
            Intent intent = new Intent(ListActivity.this, ReadActivity.class);

            // 🔹 Gửi dữ liệu qua Intent
            // Chúng ta sẽ dùng ID của truyện (ví dụ: "Doraemon")
            intent.putExtra("STORY_ID", story.getId());
            intent.putExtra("STORY_TITLE", story.getTenTruyen());
            intent.putExtra("STORY_AUTHOR", story.getTacGia());
            intent.putExtra("STORY_CATEGORY", story.getTheLoai());
            intent.putExtra("STORY_IMAGE_URL", story.getAnhBia());
            // ReadActivity có thể cần thêm description, nhưng Story model không có?
            // Tạm gửi tên truyện làm mô tả nếu bạn chưa có
            intent.putExtra("STORY_DESCRIPTION", "Truyện " + story.getTenTruyen());

            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();

        loadStories();
    }

    private void loadStories() {
        db.collection("Truyen")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    storyList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Story story = doc.toObject(Story.class);
                        story.setId(doc.getId());
                        storyList.add(story);
                    }
                    storyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show()
                );
    }
}
