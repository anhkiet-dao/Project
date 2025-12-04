package com.example.do_an.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.application.constant.FirebaseCollectionPaths;
import com.example.do_an.application.constant.FirebaseConstant;
import com.example.do_an.history.group.HistoryGroup;
import com.example.do_an.history.group.HistoryGroupAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

public class HistoryFragment extends Fragment {

    private HistoryGroupAdapter historyGroupAdapter;
    private HistoryRepository historyRepository;
//    private Button btnBack;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.history_activity_history, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyGroupAdapter = new HistoryGroupAdapter();
        recyclerView.setAdapter(historyGroupAdapter);

        historyRepository = new HistoryRepository();
        loadHistoryFromFirebase();

        return view;
    }

    private void loadHistoryFromFirebase() {
        String emailKey = getCurrentUserKey();

        if (emailKey == null) {
            Toast.makeText(requireContext(), "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference ref = buildHistoryRef(emailKey);
        ref.addListenerForSingleValueEvent(historyListener);
    }

    private final ValueEventListener historyListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            List<HistoryGroup> groups = historyRepository.processSnapshot(snapshot);
            historyGroupAdapter.submitList(groups);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
        }
    };

    private String getCurrentUserKey() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        return Objects.requireNonNull(user.getEmail()).replace(".", "_");
    }

    private DatabaseReference buildHistoryRef(String emailKey) {
        return FirebaseDatabase.getInstance(FirebaseConstant.URL)
                .getReference(FirebaseCollectionPaths.HISTORY)
                .child(emailKey);
    }
}
