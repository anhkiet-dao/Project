package com.example.do_an.Series;

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

import com.example.do_an.UI.ReadFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SeriesFragment extends Fragment {

    private RecyclerView recyclerView;
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

                        .replace(R.id.fragment_container, readFragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        }

        db = FirebaseFirestore.getInstance();
        loadSeries();
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
                        Toast.makeText(getContext(), "Chưa có tập nào!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}