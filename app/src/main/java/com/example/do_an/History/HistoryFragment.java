package com.example.do_an.History;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone; // Thêm import này cho xử lý múi giờ

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryGroupAdapter adapter;
    private ArrayList<HistoryGroup> groupList = new ArrayList<>();
    private Button btnBack;
    private TextView tvEmptyHistory; // ⬅️ THÊM: Biến cho trạng thái rỗng

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.history_activity_history, container, false);

        recyclerView = view.findViewById(R.id.recyclerHistory);
        tvEmptyHistory = view.findViewById(R.id.tvEmptyHistory); // ⬅️ THÊM: Ánh xạ TextView
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
            // ⬅️ XỬ LÝ: Không đăng nhập
            if (tvEmptyHistory != null) {
                recyclerView.setVisibility(View.GONE);
                tvEmptyHistory.setText("Bạn chưa đăng nhập!");
                tvEmptyHistory.setVisibility(View.VISIBLE);
            }
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
                Map<String, ArrayList<HistoryItem>> mapDay = new HashMap<>();

                // Định dạng đầy đủ (HH:mm:ss dd/MM/yyyy)
                SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfDay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                for (DataSnapshot item : snapshot.getChildren()) {
                    String title = Encryption.decrypt(item.child("title").getValue(String.class)); // tên truyện chính
                    String author = Encryption.decrypt(item.child("author").getValue(String.class));
                    String episodeTitle = Encryption.decrypt(item.child("episodeTitle").getValue(String.class)); // ✅ thêm tên tập
                    String startTimeStr = Encryption.decrypt(item.child("startTime").getValue(String.class));
                    String endTimeStr = Encryption.decrypt(item.child("endTime").getValue(String.class));
                    String imageUrl = Encryption.decrypt(item.child("imageUrl").getValue(String.class)); // ⬅️ TẢI imageUrl

                    Date startDate = new Date();
                    Date endDate = new Date();
                    try {
                        if (startTimeStr != null) {
                            // 💡 Gợi ý xử lý múi giờ: Nếu giờ sai (ví dụ lệch 7 tiếng), bạn có thể
                            // đặt TimeZone cho sdfFull trước khi parse, ví dụ:
                            // sdfFull.setTimeZone(TimeZone.getTimeZone("UTC"));
                            startDate = sdfFull.parse(startTimeStr);
                            // Nếu đã đặt TimeZone, nên đặt lại về TimeZone mặc định của thiết bị:
                            // sdfFull.setTimeZone(TimeZone.getDefault());
                        }
                        if (endTimeStr != null) endDate = sdfFull.parse(endTimeStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String dateKey = sdfDay.format(startDate);

                    String displayTitle = (episodeTitle != null && !episodeTitle.isEmpty())
                            ? title + " - " + episodeTitle
                            : title;

                    HistoryItem historyItem = new HistoryItem(
                            displayTitle, // ⭐️ dùng displayTitle
                            author != null ? author : "—",
                            sdfTime.format(startDate),
                            sdfTime.format(endDate),
                            imageUrl // ⬅️ THÊM imageUrl vào constructor
                    );

                    if (!mapDay.containsKey(dateKey)) mapDay.put(dateKey, new ArrayList<>());
                    mapDay.get(dateKey).add(historyItem);
                }

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

                groupList.clear();
                for (String date : sortedDates) {
                    groupList.add(new HistoryGroup(date, mapDay.get(date)));
                }

                // ⬅️ THÊM: LOGIC KIỂM TRA VÀ HIỂN THỊ THÔNG BÁO RỖNG
                if (groupList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    if (tvEmptyHistory != null) {
                        tvEmptyHistory.setText("Chưa có lịch sử xem.");
                        tvEmptyHistory.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvEmptyHistory != null) {
                        tvEmptyHistory.setVisibility(View.GONE);
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show();
                // ⬅️ XỬ LÝ: Có lỗi tải dữ liệu
                if (tvEmptyHistory != null) {
                    tvEmptyHistory.setText("Lỗi tải dữ liệu. Vui lòng kiểm tra kết nối.");
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            }
        });
    }

    // 🔹 Model (Giữ nguyên)
    public static class HistoryItem {
        String title, author, startTime, endTime, imageUrl;
        public HistoryItem(String title, String author, String startTime, String endTime, String imageUrl) {
            this.title = title;
            this.author = author;
            this.startTime = startTime;
            this.endTime = endTime;
            this.imageUrl = imageUrl;
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

    public static class HistoryGroupAdapter extends RecyclerView.Adapter<HistoryGroupAdapter.GroupViewHolder> {

        private final ArrayList<HistoryGroup> list;
        public HistoryGroupAdapter(ArrayList<HistoryGroup> list) { this.list = list; }

        @NonNull
        @Override
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

    public static class HistoryItemAdapter extends RecyclerView.Adapter<HistoryItemAdapter.ViewHolder> {

        private final ArrayList<HistoryItem> list;
        public HistoryItemAdapter(ArrayList<HistoryItem> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.history_item_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HistoryItem item = list.get(position);

            // HIỂN THỊ TÊN TRUYỆN VÀ TÁC GIẢ Ở 2 DÒNG KHÁC NHAU
            holder.tvTitle.setText(item.title);
            holder.tvAuthor.setText("Tác giả: " + item.author);

            // CHỈ HIỂN THỊ GIỜ BẮT ĐẦU
            holder.tvTime.setText("Bắt đầu: " + item.startTime);

            // HIỂN THỊ ẢNH BÌA
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.loading)
                        .error(R.drawable.ic_launcher_background)
                        .into(holder.ivCoverImage);
            } else {
                holder.ivCoverImage.setImageResource(R.drawable.ic_launcher_background);
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

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