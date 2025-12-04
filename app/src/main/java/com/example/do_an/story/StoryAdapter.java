package com.example.do_an.story;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;

public class StoryAdapter extends ListAdapter<Story, StoryAdapter.StoryViewHolder> {

    private static final String TAG = "StoryAdapter";
    private OnStoryClickListener listener = story -> {};

    public interface OnStoryClickListener {
        void onStoryClick(Story story);
    }

    public void setOnStoryClickListener(OnStoryClickListener listener) {
        this.listener = listener;
    }

    public StoryAdapter() {
        super(new StoryDiffCallback());
    }

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // use parent.getContext() to ensure correct themed context and avoid unexpected nulls
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = getItem(position);
        
        String title = orDefault(story.getTitle(), "(Không có tiêu đề)");
        String author = orDefault(story.getAuthor(), "Chưa rõ");
        String year = orDefault(story.getYear(), "N/A");
        String genre = orDefault(story.getGenre(), "");
        String thumbnail = story.getThumbnail();

        holder.tvTitle.setText(title);
        holder.tvAuthor.setText(String.format("Tác giả: %s", author));
        holder.tvYear.setText(String.format("Năm phát hành: %s", year));
        holder.tvGenre.setText(genre);

        if (isBlank(thumbnail)) {
            holder.imgThumbnail.setImageResource(R.drawable.bg_image_placeholder);
        } else {
            String imageUrl = convertGoogleDriveUrl(thumbnail);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.imgThumbnail);
        }

        holder.itemView.setOnClickListener(v -> listener.onStoryClick(story));
    }

    private String orDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String convertGoogleDriveUrl(String url) {
        if (!url.contains("drive.google.com")) {
            return url;
        }

        String fileId = "";
        if (url.contains("/d/")) {
            fileId = url.split("/d/")[1].split("/")[0];
        } else if (url.contains("id=")) {
            fileId = url.substring(url.indexOf("id=") + 3);
        }

        return fileId.isEmpty() ? url : "https://drive.google.com/uc?export=view&id=" + fileId;
    }

    public static class StoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgThumbnail;
        final TextView tvTitle;
        final TextView tvAuthor;
        final TextView tvYear;
        final TextView tvGenre;

        public StoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvAuthor = itemView.findViewById(R.id.tv_author);
            tvYear = itemView.findViewById(R.id.tv_year);
            tvGenre = itemView.findViewById(R.id.tv_genre);
        }
    }
}
