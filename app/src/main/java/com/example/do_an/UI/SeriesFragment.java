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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.application.ReadActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.do_an.Story.Series;
import com.example.do_an.Story.SeriesAdapter;
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
        return inflater.inflate(R.layout.activity_series, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nhận dữ liệu từ Bundle
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
                Intent intent = new Intent(getContext(), ReadActivity.class);

                intent.putExtra("STORY_ID", storyId);
                intent.putExtra("STORY_TITLE", storyName);
                intent.putExtra("STORY_AUTHOR", storyAuthor);
                intent.putExtra("STORY_CATEGORY", storyCategory);
                intent.putExtra("STORY_DESCRIPTION", storyDescription);
                intent.putExtra("STORY_IMAGE_URL", storyImageUrl);

                intent.putExtra("PDF_LINK", series.getLink());
                intent.putExtra("TAP", series.getName());

                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }

        db = FirebaseFirestore.getInstance();
        loadSeries();
    }

    private void loadSeries() {
        if (storyId == null) return;

        db.collection("story")
                .document(storyId)
                .collection("Series")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;

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
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}