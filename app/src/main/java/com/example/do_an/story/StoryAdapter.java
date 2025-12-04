package com.example.do_an.story;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.application.util.GoogleDriveUtil;
import com.example.do_an.application.util.StringUtil;

import java.util.Objects;

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
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Story> DIFF_CALLBACK = new DiffUtil.ItemCallback<Story>() {
        @Override
        public boolean areItemsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Story oldItem, @NonNull Story newItem) {
            return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getAuthor(), newItem.getAuthor())
                    && Objects.equals(oldItem.getYear(), newItem.getYear())
                    && Objects.equals(oldItem.getGenre(), newItem.getGenre())
                    && Objects.equals(oldItem.getThumbnail(), newItem.getThumbnail());
        }
    };

    @NonNull
    @Override
    public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.story_item_story, parent, false);
        return new StoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
        Story story = getItem(position);

        String title = StringUtil.orDefault(story.getTitle(), "(Không có tiêu đề)");
        String author = StringUtil.orDefault(story.getAuthor(), "Chưa rõ");
        String year = StringUtil.orDefault(story.getYear(), "N/A");
        String genre = StringUtil.orDefault(story.getGenre(), "");
        String thumbnail = story.getThumbnail();

        holder.tvTitle.setText(title);
        holder.tvAuthor.setText(String.format("Tác giả: %s", author));
        holder.tvYear.setText(String.format("Năm phát hành: %s", year));
        holder.tvGenre.setText(genre);

        if (StringUtil.isBlank(thumbnail)) {
            holder.imgThumbnail.setImageResource(R.drawable.bg_image_placeholder);
        } else {
            String imageUrl = GoogleDriveUtil.convertGoogleDriveUrl(thumbnail);

            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.imgThumbnail);
        }

        holder.itemView.setOnClickListener(v -> listener.onStoryClick(story));
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
