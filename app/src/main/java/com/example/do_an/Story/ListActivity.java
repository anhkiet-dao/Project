package com.example.do_an.Story;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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

        // Quan trọng: truyền đúng STORY_ID khi click
        storyAdapter.setOnStoryClickListener(story -> {
            Intent intent = new Intent(ListActivity.this, SeriesActivity.class);
            intent.putExtra("STORY_ID", story.getId());        // ID để query subcollection
            intent.putExtra("STORY_NAME", story.getTenTruyen());
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
                        if (story != null) {
                            story.setId(doc.getId()); // RẤT QUAN TRỌNG!
                            storyList.add(story);
                        }
                    }
                    storyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}