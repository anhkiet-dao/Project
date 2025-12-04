package com.example.do_an.story;

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
import com.example.do_an.core.constants.FirebaseCollectionPaths;
import com.example.do_an.story.constants.StoryBundleConstants;
import com.example.do_an.series.SeriesFragment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StoryListFragment extends Fragment {

    private StoryAdapter storyAdapter;
    private final List<Story> allStories = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup viewGroup, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.story_activity_readlist, viewGroup, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewStories);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        storyAdapter = new StoryAdapter();
        recyclerView.setAdapter(storyAdapter);

        loadStoriesFromFirebase();

        storyAdapter.setOnStoryClickListener(story -> {
            SeriesFragment seriesFragment = new SeriesFragment();
            Bundle args = new Bundle();
            args.putString(StoryBundleConstants.ID, story.getId());
            args.putString(StoryBundleConstants.NAME, story.getTitle());
            args.putString(StoryBundleConstants.AUTHOR, story.getAuthor());
            args.putString(StoryBundleConstants.GENRE, story.getGenre());
            args.putString(StoryBundleConstants.IMAGE_URL, story.getThumbnail());
            seriesFragment.setArguments(args);

            if (getParentFragment() != null) {
                getParentFragment().getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_main, seriesFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        return view;
    }

    private void loadStoriesFromFirebase() {
        FirebaseFirestore.getInstance()
                .collection(FirebaseCollectionPaths.STORY)
                .get()
                .addOnSuccessListener(snapshots -> {
                    allStories.clear();

                    for (DocumentSnapshot doc : snapshots) {
                        Story story = doc.toObject(Story.class);
                        if (story != null) {
                            story.setId(doc.getId());
                            allStories.add(story);
                        }
                    }

                    storyAdapter.submitList(new ArrayList<>(allStories));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                });
    }

    public void onFilterKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            storyAdapter.submitList(new ArrayList<>(allStories));
            return;
        }

        String lowerKeyword = keyword.toLowerCase();
        List<Story> filtered = allStories.stream()
                .filter(s -> s.getTitle() != null && s.getTitle().toLowerCase().contains(lowerKeyword))
                .collect(Collectors.toList());

        storyAdapter.submitList(filtered);
    }
}
