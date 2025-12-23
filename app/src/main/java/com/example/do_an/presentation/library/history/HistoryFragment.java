package com.example.do_an.presentation.library.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.core.constant.FirebaseConstants;
import com.example.do_an.presentation.library.history.adapter.HistoryGroupAdapter;
import com.example.do_an.domain.library.model.HistoryGroup;
import com.example.do_an.domain.library.model.HistoryItem;
import com.example.do_an.domain.library.model.HistoryItemWithDate;
import com.example.do_an.presentation.library.history.util.HistoryDataProcessor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerHistory;
    private TextView textEmptyHistory;

    private HistoryGroupAdapter adapter;
    private ArrayList<HistoryGroup> groupList = new ArrayList<>();

    private FirebaseAuth auth;
    private DatabaseReference databaseRef;
    private HistoryDataProcessor dataProcessor;

    private boolean isViewCreated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.history_fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;

        bindViews(view);
        initDependencies();
        setupUi();
        bindActions();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    // =========================================================
    // 1️⃣ Setup phase
    // =========================================================

    private void bindViews(View view) {
        recyclerHistory = view.findViewById(R.id.recyclerHistory);
        textEmptyHistory = view.findViewById(R.id.textEmptyHistory);
    }

    private void initDependencies() {
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
                .getReference("History");

        dataProcessor = new HistoryDataProcessor();
        adapter = new HistoryGroupAdapter(groupList);
    }

    private void setupUi() {
        recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerHistory.setAdapter(adapter);

        loadHistoryData();
    }

    private void bindActions() {
        // No user actions in this fragment
    }

    // =========================================================
    // 2️⃣ UI helpers
    // =========================================================

    private void showEmptyState(String message) {
        recyclerHistory.setVisibility(View.GONE);
        textEmptyHistory.setText(message);
        textEmptyHistory.setVisibility(View.VISIBLE);
    }

    private void showHistoryList() {
        textEmptyHistory.setVisibility(View.GONE);
        recyclerHistory.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
    }

    // =========================================================
    // 3️⃣ Data loading
    // =========================================================

    private void loadHistoryData() {
        if (!isViewCreated || getContext() == null)
            return;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            onLoadFailed(getString(R.string.history_empty_no_login));
            return;
        }

        String emailKey = currentUser.getEmail().replace(".", "_");
        DatabaseReference userHistoryRef = databaseRef.child(emailKey);

        userHistoryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isViewCreated || getContext() == null)
                    return;
                onDataLoaded(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isViewCreated || getContext() == null)
                    return;
                onLoadFailed(getString(R.string.history_error_load));
            }
        });
    }

    // =========================================================
    // 4️⃣ Data processing
    // =========================================================

    private void onDataLoaded(DataSnapshot snapshot) {
        ArrayList<HistoryItemWithDate> allHistoryItems = dataProcessor.parseHistoryItems(snapshot);
        dataProcessor.sortByDateDescending(allHistoryItems);

        Map<String, ArrayList<HistoryItem>> groupedByDate = dataProcessor.groupItemsByDate(allHistoryItems);
        ArrayList<String> sortedDates = dataProcessor.sortDates(groupedByDate.keySet());

        updateGroupList(sortedDates, groupedByDate);
        displayHistory();
    }

    private void updateGroupList(ArrayList<String> sortedDates, Map<String, ArrayList<HistoryItem>> groupedByDate) {
        groupList.clear();
        for (String date : sortedDates) {
            groupList.add(new HistoryGroup(date, groupedByDate.get(date)));
        }
    }

    // =========================================================
    // 5️⃣ Result handlers
    // =========================================================

    private void displayHistory() {
        if (!isViewCreated)
            return;

        if (groupList.isEmpty()) {
            showEmptyState(getString(R.string.history_empty_no_data));
        } else {
            showHistoryList();
        }
    }

    private void onLoadFailed(String message) {
        showEmptyState(message);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}
