package com.example.do_an.History;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.application.Encryption;
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
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryGroupAdapter adapter;
    private ArrayList<HistoryGroup> groupList = new ArrayList<>();
    private TextView tvEmptyHistory, tvTitle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_fragment_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistory);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory);
        tvTitle = view.findViewById(R.id.tvTitle);

        tvTitle.setText(getString(R.string.history_title)); // 🌍 đa ngôn ngữ

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryGroupAdapter(groupList);
        recyclerView.setAdapter(adapter);

        loadHistoryFromFirebase();
        return view;
    }

    private void loadHistoryFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), getString(R.string.history_empty_no_login), Toast.LENGTH_SHORT).show();
            recyclerView.setVisibility(View.GONE);
            tvEmptyHistory.setText(getString(R.string.history_empty_no_login));
            tvEmptyHistory.setVisibility(View.VISIBLE);
            return;
        }

        String emailKey = currentUser.getEmail().replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance(
                        "https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("History").child(emailKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();

                SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                ArrayList<HistoryItemWithDate> allHistoryItems = new ArrayList<>();

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String title = Encryption.decrypt(itemSnapshot.child("title").getValue(String.class));
                    String author = Encryption.decrypt(itemSnapshot.child("author").getValue(String.class));
                    String episodeTitle = Encryption.decrypt(itemSnapshot.child("episodeTitle").getValue(String.class));
                    String startTimeStr = Encryption.decrypt(itemSnapshot.child("startTime").getValue(String.class));
                    String endTimeStr = Encryption.decrypt(itemSnapshot.child("endTime").getValue(String.class));
                    String imageUrl = Encryption.decrypt(itemSnapshot.child("imageUrl").getValue(String.class));

                    Date startDate = null, endDate = null;

                    try {
                        if (startTimeStr != null) startDate = sdfFull.parse(startTimeStr);
                        if (endTimeStr != null) endDate = sdfFull.parse(endTimeStr);
                    } catch (Exception e) { continue; }

                    if (startDate == null) continue;

                    String displayTitle = (episodeTitle != null && !episodeTitle.isEmpty())
                            ? title + " - " + episodeTitle
                            : title;

                    HistoryItem item = new HistoryItem(
                            displayTitle,
                            author != null ? author : "—",
                            sdfTime.format(startDate),
                            endDate != null ? sdfTime.format(endDate) : "—",
                            imageUrl
                    );

                    allHistoryItems.add(new HistoryItemWithDate(item, startDate));
                }

                Collections.sort(allHistoryItems, (i1, i2) -> i2.date.compareTo(i1.date));

                Map<String, ArrayList<HistoryItem>> mapDay = new HashMap<>();

                for (HistoryItemWithDate itemWithDate : allHistoryItems) {
                    String dateKey = sdfDay.format(itemWithDate.date);
                    if (!mapDay.containsKey(dateKey)) mapDay.put(dateKey, new ArrayList<>());
                    if (mapDay.get(dateKey).size() < 10) {
                        mapDay.get(dateKey).add(itemWithDate.item);
                    }
                }

                ArrayList<String> sortedDates = new ArrayList<>(mapDay.keySet());
                Collections.sort(sortedDates, (d1, d2) -> {
                    try {
                        return sdfDay.parse(d2).compareTo(sdfDay.parse(d1));
                    } catch (Exception e) { return 0; }
                });

                groupList.clear();
                for (String date : sortedDates) {
                    groupList.add(new HistoryGroup(date, mapDay.get(date)));
                }

                if (groupList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmptyHistory.setText(getString(R.string.history_empty_no_data));
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyHistory.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), getString(R.string.history_error_load), Toast.LENGTH_SHORT).show();
                recyclerView.setVisibility(View.GONE);
                tvEmptyHistory.setText(getString(R.string.history_error_load));
                tvEmptyHistory.setVisibility(View.VISIBLE);
            }
        });
    }

    public static class HistoryItemWithDate {
        final HistoryItem item;
        final Date date;
        public HistoryItemWithDate(HistoryItem item, Date date) { this.item = item; this.date = date; }
    }

    public static class HistoryItem {
        String title, author, startTime, endTime, imageUrl;
        public HistoryItem(String title, String author, String startTime, String endTime, String imageUrl) {
            this.title = title; this.author = author;
            this.startTime = startTime; this.endTime = endTime; this.imageUrl = imageUrl;
        }
    }

    public static class HistoryGroup {
        String date;
        ArrayList<HistoryItem> items;
        public HistoryGroup(String date, ArrayList<HistoryItem> items) { this.date = date; this.items = items; }
    }

    // ------- Adapter Group -------
    public static class HistoryGroupAdapter extends RecyclerView.Adapter<HistoryGroupAdapter.GroupViewHolder> {

        private final ArrayList<HistoryGroup> list;
        public HistoryGroupAdapter(ArrayList<HistoryGroup> list) { this.list = list; }

        @NonNull @Override
        public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.history_item_history_group, parent, false);
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

        @Override public int getItemCount() { return list.size(); }

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

    // ------- Adapter Item -------
    public static class HistoryItemAdapter extends RecyclerView.Adapter<HistoryItemAdapter.ViewHolder> {

        private final ArrayList<HistoryItem> list;
        public HistoryItemAdapter(ArrayList<HistoryItem> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.history_item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = list.get(position);

            holder.tvTitle.setText(item.title);
            holder.tvAuthor.setText(holder.itemView.getContext().getString(R.string.history_author, item.author));
            holder.tvTime.setText(holder.itemView.getContext().getString(R.string.history_start_time, item.startTime));

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_launcher_background)
                        .into(holder.ivCoverImage);
            } else {
                holder.ivCoverImage.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvAuthor, tvTime;
            ImageView ivCoverImage;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvAuthor = itemView.findViewById(R.id.tvAuthor);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivCoverImage = itemView.findViewById(R.id.ivCoverImage);
            }
        }
    }
}
