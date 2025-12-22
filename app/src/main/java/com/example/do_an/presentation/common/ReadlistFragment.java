package com.example.do_an.presentation.common;

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
import com.example.do_an.presentation.library.series.SeriesFragment;
import com.example.do_an.domain.library.model.Story;
import com.example.do_an.presentation.library.series.adapter.StoryAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ReadlistFragment extends Fragment {

    private RecyclerView recyclerView;
    private StoryAdapter storyAdapter;
    private final List<Story> storyList = new ArrayList<>();
    private final List<Story> filteredList = new ArrayList<>();

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ui_activity_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewStories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        storyAdapter = new StoryAdapter(getContext(), filteredList);
        recyclerView.setAdapter(storyAdapter);

        storyAdapter.setOnStoryClickListener(story -> {
            SeriesFragment seriesFragment = new SeriesFragment();

            Bundle args = new Bundle();
            args.putString("STORY_ID", story.getId());
            args.putString("STORY_NAME", story.getTenTruyen());
            args.putString("STORY_AUTHOR", story.getTacGia());
            args.putString("STORY_CATEGORY", story.getTheLoai());
            args.putString("STORY_IMAGE_URL", story.getAnhBia());

            seriesFragment.setArguments(args);

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.containerContent, seriesFragment)
                        .addToBackStack(null)
                        .commit();
            }
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
