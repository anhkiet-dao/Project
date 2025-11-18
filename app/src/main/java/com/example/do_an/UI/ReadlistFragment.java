package com.example.do_an.UI;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.Story.ListActivity;
import com.example.do_an.Story.SeriesActivity;
import com.example.do_an.Story.Story;
import com.example.do_an.Story.StoryAdapter;
import com.example.do_an.application.ReadActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.do_an.Story.SeriesActivity;

import java.util.ArrayList;
import java.util.List;

public class ReadlistFragment extends Fragment {

    private RecyclerView recyclerView;
    private StoryAdapter storyAdapter;

    // Danh sách gốc & danh sách hiển thị
    private final List<Story> storyList = new ArrayList<>();
    private final List<Story> filteredList = new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewStories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Adapter dùng filteredList
        storyAdapter = new StoryAdapter(getContext(), filteredList);
        recyclerView.setAdapter(storyAdapter);

        // Sự kiện click vào 1 truyện
        storyAdapter.setOnStoryClickListener(story -> {
            Intent intent = new Intent(getContext(), SeriesActivity.class);
            intent.putExtra("STORY_ID", story.getId());        // ID để query subcollection
            intent.putExtra("STORY_NAME", story.getTenTruyen());
            intent.putExtra("STORY_AUTHOR", story.getTacGia());
            intent.putExtra("STORY_CATEGORY", story.getTheLoai());
            intent.putExtra("STORY_IMAGE_URL", story.getAnhBia());
            intent.putExtra("STORY_YEAR", story.getNamPhatHanh());
            startActivity(intent);
        });

        db = FirebaseFirestore.getInstance();
        loadStories();

        return view;
    }

    private void loadStories() {
        db.collection("Truyen")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    storyList.clear();
                    filteredList.clear();

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Story story = doc.toObject(Story.class);
                            if (story != null) {
                                story.setId(doc.getId());
                                storyList.add(story);
                            }
                        }

                        // Ban đầu hiển thị toàn bộ
                        filteredList.addAll(storyList);
                        storyAdapter.notifyDataSetChanged();

                    } else {
                        Toast.makeText(getContext(), "Không có truyện nào!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ----------------------------------------------------
    // 🔍 Hàm FILTER để MyList gọi khi người dùng nhập search
    // ----------------------------------------------------
    public void onSearch(String keyword) {
        filteredList.clear();

        if (keyword.isEmpty()) {
            filteredList.addAll(storyList);
        } else {
            for (Story s : storyList) {
                if (s.getTenTruyen().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredList.add(s);
                }
            }
        }

        storyAdapter.notifyDataSetChanged();
    }
}
