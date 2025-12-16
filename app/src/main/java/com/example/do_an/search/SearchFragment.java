package com.example.do_an.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView resultsRecyclerView;
    private Button searchButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Gắn layout fragment_search.xml
        View view = inflater.inflate(R.layout.search_fragment_search, container, false); // Đảm bảo bạn đang sử dụng R.layout.fragment_search (hoặc tên layout chính xác)

        searchView = view.findViewById(R.id.search_view);
        resultsRecyclerView = view.findViewById(R.id.results_recycler_view);
        searchButton = view.findViewById(R.id.btn_search_execute);

        setupSearchViewAndButton();

        return view;
    }

    private void setupSearchViewAndButton() {
        searchButton.setOnClickListener(v -> {
            String query = searchView.getQuery().toString();
            performSearch(query);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void performSearch(String query) {

        List<String> filteredList = filterData(query);

        if (!query.isEmpty()) {
            resultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            resultsRecyclerView.setVisibility(View.GONE);
        }
    }

    private List<String> filterData(String query) {
        List<String> originalList = getOriginalData();
        List<String> filteredList = new ArrayList<>();

        if (query.isEmpty()) {
            return originalList;
        }

        String lowerCaseQuery = query.toLowerCase();
        for (String item : originalList) {
            if (item.toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(item);
            }
        }
        return filteredList;
    }

    private List<String> getOriginalData() {
        List<String> data = new ArrayList<>();
        data.add("Apple");
        data.add("Banana");
        data.add("Cherry");
        data.add("Date");
        data.add("Elderberry");
        return data;
    }
}