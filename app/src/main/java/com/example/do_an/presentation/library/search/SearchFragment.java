package com.example.do_an.presentation.library.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.core.network.RetrofitClient;
import com.example.do_an.data.library.remote.SearchApi;
import com.example.do_an.data.library.remote.dto.SearchRequest;
import com.example.do_an.data.library.remote.dto.SearchResponse;
import com.example.do_an.presentation.library.series.SeriesFragment;
import com.example.do_an.domain.library.model.Story;
import com.example.do_an.presentation.library.series.adapter.StoryAdapter;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView resultsRecyclerView;
    private Button searchButton;
    private ProgressBar progressBar;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment_search, container, false);

        searchView = view.findViewById(R.id.search_view);
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view);
        searchButton = view.findViewById(R.id.btn_search_execute);
        progressBar = view.findViewById(R.id.progress_bar);

        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestore = FirebaseFirestore.getInstance();

        setupSearchViewAndButton();
        return view;
    }

    private void setupSearchViewAndButton() {
        searchButton.setOnClickListener(v -> performSearch(searchView.getQuery().toString().trim()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query.trim());
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) { return false; }
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        resultsRecyclerView.setVisibility(View.GONE);

        SearchApi api = RetrofitClient.getClient().create(SearchApi.class);
        SearchRequest request = new SearchRequest(query);

        api.searchStory(request).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                SearchResponse result = response.body();
                List<String> storyNames = new ArrayList<>();

                if (result.stories != null && !result.stories.isEmpty()) {
                    storyNames.addAll(result.stories);
                } else if (result.story != null) {
                    storyNames.add(result.story);
                }

                if (!storyNames.isEmpty()) {
                    fetchMultipleStoriesFromFirestore(storyNames);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("SEARCH_DEBUG", "Retrofit Failure: " + t.getMessage());
            }
        });
    }

    private void fetchMultipleStoriesFromFirestore(List<String> storyNames) {
        List<String> limitedNames = storyNames.size() > 10 ? storyNames.subList(0, 10) : storyNames;

        firestore.collection("Truyen")
                .whereIn(FieldPath.documentId(), limitedNames)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Story> storyList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Story story = document.toObject(Story.class);
                        story.setId(document.getId());
                        storyList.add(story);
                    }
                    updateUI(storyList);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("SEARCH_DEBUG", "Firestore Error: " + e.getMessage());
                });
    }

    private void updateUI(List<Story> list) {
        progressBar.setVisibility(View.GONE);

        if (list.isEmpty()) {
            resultsRecyclerView.setVisibility(View.GONE);
            return;
        }

        StoryAdapter adapter = new StoryAdapter(getContext(), list);

        adapter.setOnStoryClickListener(new StoryAdapter.OnStoryClickListener() {
            @Override
            public void onStoryClick(Story story) {
                navigateToSeries(story);
            }
        });

        resultsRecyclerView.setAdapter(adapter);
        resultsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void navigateToSeries(Story story) {
        SeriesFragment fragment = new SeriesFragment();
        Bundle args = new Bundle();

        args.putString("STORY_ID", story.getId());
        args.putString("STORY_NAME", story.getTenTruyen());
        args.putString("STORY_AUTHOR", story.getTacGia());
        args.putString("STORY_IMAGE_URL", story.getAnhBia());

        fragment.setArguments(args);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}