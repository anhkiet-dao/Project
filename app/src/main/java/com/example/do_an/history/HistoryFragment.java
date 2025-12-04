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
import com.example.do_an.application.Encryption;
import com.example.do_an.application.constant.FirebaseCollectionPaths;
import com.example.do_an.application.constant.FirebaseConstant;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HistoryFragment extends Fragment {

    private HistoryGroupAdapter historyGroupAdapter;
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
            List<HistoryItemRaw> rawItems = parseSnapshot(snapshot);
            Map<String, List<HistoryItem>> grouped = groupItemsByDate(rawItems);
            List<HistoryGroup> sortedGroups = sortAndConvert(grouped);
            updateUI(sortedGroups);
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(requireContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
        }
    };

    private List<HistoryItemRaw> parseSnapshot(DataSnapshot snapshot) {
        List<HistoryItemRaw> list = new ArrayList<>();

        for (DataSnapshot child : snapshot.getChildren()) {
            String title = decrypt(child, "title");
            String author = decrypt(child, "author");
            String episodeTitle = decrypt(child, "episodeTitle");
            String start = decrypt(child, "startTime");
            String end = decrypt(child, "endTime");

            if (start == null || end == null) {
                continue;
            }

            list.add(new HistoryItemRaw(title, author, episodeTitle, start, end));
        }

        return list;
    }

    private Map<String, List<HistoryItem>> groupItemsByDate(List<HistoryItemRaw> rawList) {
        Map<String, List<HistoryItem>> map = new HashMap<>();

        SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (HistoryItemRaw raw : rawList) {
            try {
                Date start = sdfFull.parse(raw.startTime);
                Date end = sdfFull.parse(raw.endTime);

                if (start == null || end == null) {
                    continue;
                }

                String dateKey = sdfDate.format(start);
                String displayTitle = createDisplayTitle(raw);

                HistoryItem item = new HistoryItem(
                        displayTitle,
                        raw.author != null ? raw.author : "—",
                        sdfTime.format(start),
                        sdfTime.format(end)
                );

                map.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(item);

            } catch (Exception ignored) {
            }
        }

        return map;
    }

    private String createDisplayTitle(HistoryItemRaw raw) {
        return (raw.episodeTitle != null && !raw.episodeTitle.isEmpty())
                ? raw.title + " - " + raw.episodeTitle
                : raw.title;
    }


    private String decrypt(DataSnapshot snap, String key) {
        return Encryption.decrypt(snap.child(key).getValue(String.class));
    }

    private List<HistoryGroup> sortAndConvert(Map<String, List<HistoryItem>> map) {
        List<String> dates = new ArrayList<>(map.keySet());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        dates.sort((d1, d2) -> {
            try {
                return Objects.requireNonNull(sdf.parse(d2)).compareTo(sdf.parse(d1));
            } catch (Exception e) {
                return 0;
            }
        });

        List<HistoryGroup> groups = new ArrayList<>();
        for (String date : dates) {
            groups.add(new HistoryGroup(date, (ArrayList<HistoryItem>) map.get(date)));
        }

        return groups;
    }

    private void updateUI(List<HistoryGroup> groups) {
        historyGroupAdapter.submitList(groups);
    }

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
