package com.example.do_an.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryGroupAdapter adapter;
    private ArrayList<HistoryGroup> groupList = new ArrayList<>();
    private Button btnBack;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Nạp giao diện cho Fragment
        View view = inflater.inflate(R.layout.activity_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new HistoryGroupAdapter(groupList);
        recyclerView.setAdapter(adapter);

        loadHistoryFromFirebase();

        return view;
    }

    private void loadHistoryFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        String emailKey = currentUser.getEmail().replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance(
                        "https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("History").child(emailKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();
                Map<String, ArrayList<HistoryItem>> mapDay = new HashMap<>();

                SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                for (DataSnapshot item : snapshot.getChildren()) {
                    String title = item.child("title").getValue(String.class); // tên truyện chính
                    String author = item.child("author").getValue(String.class);
                    String episodeTitle = item.child("episodeTitle").getValue(String.class); // ✅ thêm tên tập
                    String startTimeStr = item.child("startTime").getValue(String.class);
                    String endTimeStr = item.child("endTime").getValue(String.class);

                    Date startDate = new Date();
                    Date endDate = new Date();
                    try {
                        if (startTimeStr != null) startDate = sdfFull.parse(startTimeStr);
                        if (endTimeStr != null) endDate = sdfFull.parse(endTimeStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String dateKey = sdfDay.format(startDate);

                    // Hiển thị: Tên truyện chính + tên tập
                    String displayTitle = (episodeTitle != null && !episodeTitle.isEmpty())
                            ? title + " - " + episodeTitle
                            : title;

                    HistoryItem historyItem = new HistoryItem(
                            displayTitle, // ⭐️ dùng displayTitle
                            author != null ? author : "—",
                            sdfTime.format(startDate),
                            sdfTime.format(endDate)
                    );

                    if (!mapDay.containsKey(dateKey)) mapDay.put(dateKey, new ArrayList<>());
                    mapDay.get(dateKey).add(historyItem);
                }

                // Sắp xếp ngày từ mới đến cũ
                List<String> sortedDates = new ArrayList<>(mapDay.keySet());
                Collections.sort(sortedDates, (d1, d2) -> {
                    try {
                        Date date1 = sdfDay.parse(d1);
                        Date date2 = sdfDay.parse(d2);
                        return date2.compareTo(date1);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                // Chuyển map sang groupList
                groupList.clear();
                for (String date : sortedDates) {
                    groupList.add(new HistoryGroup(date, mapDay.get(date)));
                }

                adapter.notifyDataSetChanged();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Model
    public static class HistoryItem {
        String title, author, startTime, endTime;
        public HistoryItem(String title, String author, String startTime, String endTime) {
            this.title = title;
            this.author = author;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    public static class HistoryGroup {
        String date;
        ArrayList<HistoryItem> items;
        public HistoryGroup(String date, ArrayList<HistoryItem> items) {
            this.date = date;
            this.items = items;
        }
    }

    // 🔹 Adapter nhóm ngày
    public static class HistoryGroupAdapter extends RecyclerView.Adapter<HistoryGroupAdapter.GroupViewHolder> {

        private final ArrayList<HistoryGroup> list;
        public HistoryGroupAdapter(ArrayList<HistoryGroup> list) { this.list = list; }

        @NonNull
        @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_group, parent, false);
            return new GroupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
            HistoryGroup group = list.get(position);
            holder.tvDate.setText(group.date);

            HistoryItemAdapter innerAdapter = new HistoryItemAdapter(group.items);
            holder.recyclerInner.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            holder.recyclerInner.setAdapter(innerAdapter);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class GroupViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate;
            RecyclerView recyclerInner;
            public GroupViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tvDate);
                recyclerInner = itemView.findViewById(R.id.recyclerInner);
            }
        }
    }

    // 🔹 Adapter từng truyện
    public static class HistoryItemAdapter extends RecyclerView.Adapter<HistoryItemAdapter.ViewHolder> {

        private final ArrayList<HistoryItem> list;
        public HistoryItemAdapter(ArrayList<HistoryItem> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = list.get(position);
            holder.tvTitleAuthor.setText(item.title + " - " + item.author);
            holder.tvTime.setText("Bắt đầu: " + item.startTime + "\nKết thúc: " + item.endTime);
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitleAuthor, tvTime;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitleAuthor = itemView.findViewById(R.id.tvTitleAuthor);
                tvTime = itemView.findViewById(R.id.tvTime);
            }
        }
    }
}
