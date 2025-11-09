package com.example.do_an.Story;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;

import java.util.List;

public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.StoryViewHolder> {

    private final List<Story> storyList;
    private final Context context;
    private OnStoryClickListener mListener;

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.mListener = listener;
    }

    public StoryAdapter(Context context, List<Story> storyList) {
        this.context = context;
        this.storyList = storyList;
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = storyList.get(position);

        holder.txtTenTruyen.setText(story.getTenTruyen());
        holder.txtTacGia.setText("Tác giả: " + story.getTacGia());
        holder.txtNamPhatHanh.setText("Năm: " + story.getNamPhatHanh());
        holder.txtTheLoai.setText("Thể loại: " + story.getTheLoai());

        String imageUrl = story.getAnhBia();

        // 🔹 Kiểm tra link ảnh
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Log.e("StoryAdapter", "Không có link ảnh cho truyện: " + story.getTenTruyen());
            holder.imgAnhBia.setImageResource(R.drawable.bg_image_placeholder);
            return;
        }

        // 🔹 Xử lý link Google Drive (chuyển sang dạng có thể load được)
        if (imageUrl.contains("drive.google.com")) {
            try {
                if (imageUrl.contains("/d/")) {
                    String fileId = imageUrl.split("/d/")[1].split("/")[0];
                    imageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                } else if (imageUrl.contains("id=")) {
                    String fileId = imageUrl.substring(imageUrl.indexOf("id=") + 3);
                    imageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                }
            } catch (Exception e) {
                Log.e("StoryAdapter", "Lỗi khi tách ID từ link Drive: " + e.getMessage());
            }
        }

        Log.d("StoryAdapter", "Đang tải ảnh từ: " + imageUrl);

        // 🔹 Sử dụng Glide để load ảnh
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.bg_image_placeholder) // hiển thị tạm khi chưa load xong
                .error(R.drawable.bg_image_placeholder)       // hiển thị nếu load lỗi
                .into(holder.imgAnhBia);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onStoryClick(story);
            }
        });
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAnhBia;
        TextView txtTenTruyen, txtTacGia, txtNamPhatHanh, txtTheLoai;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAnhBia = itemView.findViewById(R.id.imgAnhBia);
            txtTenTruyen = itemView.findViewById(R.id.txtTenTruyen);
            txtTacGia = itemView.findViewById(R.id.txtTacGia);
            txtNamPhatHanh = itemView.findViewById(R.id.txtNamPhatHanh);
            txtTheLoai = itemView.findViewById(R.id.txtTheLoai);
        }
    }
}
