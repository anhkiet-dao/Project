package com.example.do_an.series;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.application.constant.FirebaseCollectionPaths;
import com.example.do_an.main.read.ReadFragment;
import com.example.do_an.story.StoryBundleConstant;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SeriesFragment extends Fragment {

    private SeriesAdapter seriesAdapter;
    private FirebaseFirestore firestore;
    private String storyId, storyName, storyAuthor, storyCategory, storyDescription, storyImageUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.series_activity_series, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            storyId = args.getString(StoryBundleConstant.STORY_ID, "");
            storyName = args.getString(StoryBundleConstant.STORY_NAME, "");
            storyAuthor = args.getString(StoryBundleConstant.STORY_AUTHOR, "");
            storyCategory = args.getString(StoryBundleConstant.STORY_CATEGORY, "");
            storyDescription = args.getString(StoryBundleConstant.STORY_DESCRIPTION, "");
            storyImageUrl = args.getString(StoryBundleConstant.STORY_IMAGE_URL, "");
        } else {
            storyId = "";
            storyName = "";
            storyAuthor = "";
            storyCategory = "";
            storyDescription = "";
            storyImageUrl = "";
        }

        if (storyId.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy truyện!", Toast.LENGTH_SHORT).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(!storyName.isEmpty() ? storyName : "Danh sách tập");
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        RecyclerView recyclerView = view.findViewById(R.id.recyclerSeries);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        seriesAdapter = new SeriesAdapter();
        recyclerView.setAdapter(seriesAdapter);

        seriesAdapter.setOnSeriesClickListener(series -> {
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

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_main, readFragment)
                    .addToBackStack(null)
                    .commit();
        });

        firestore = FirebaseFirestore.getInstance();
        loadSeriesFromFiresbase();
    }

    private void loadSeriesFromFiresbase() {
        if (storyId.isEmpty()) return;

        firestore.collection(FirebaseCollectionPaths.STORY_SERIES)
                .document(storyId)
                .collection(FirebaseCollectionPaths.SERIES)
                .orderBy("name")
                .get()
                .addOnSuccessListener(snapshots -> {

                    if (!isAdded()) return;

                    List<Series> seriesList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Series s = doc.toObject(Series.class);
                        s.setId(doc.getId());
                        if (s.getName() != null && s.getLink() != null) {
                            seriesList.add(s);
                        }
                    }

                    seriesAdapter.submitList(seriesList);

                    if (seriesList.isEmpty()) {
                        Toast.makeText(getContext(), "Chưa có tập nào!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}