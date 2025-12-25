package com.example.do_an.presentation.library.favorites.adapter;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;
import com.example.do_an.domain.library.model.FavoriteStory;
import com.example.do_an.presentation.reading.reader.ReadFragment;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavViewHolder> {

    private final Context context;
    private final List<FavoriteStory> favoriteList;
    private OnRemoveFavoriteListener removeListener;

    public FavoriteAdapter(Context context, List<FavoriteStory> favoriteList) {
        this.context = context;
        this.favoriteList = favoriteList;
    }

    public interface OnRemoveFavoriteListener {
        void onRemoveFavorite(FavoriteStory story, int position);
    }

    public void setOnRemoveFavoriteListener(OnRemoveFavoriteListener listener) {
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public FavViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.favorite_item_favorite, parent, false);
        return new FavViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavViewHolder holder, int position) {
        FavoriteStory story = favoriteList.get(position);

        holder.textStoryTitle.setText(story.getTitle());

        String authorLabel = context.getString(R.string.author_label);
        holder.textAuthor.setText(authorLabel + story.getAuthor());

        loadStoryImage(holder.imageStory, story.getImageUrl());

        // Click listener to open ReadFragment
        holder.itemView.setOnClickListener(v -> navigateToReadFragment(story));

        // Remove favorite button
        holder.btnRemoveFav.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveFavorite(story, position);
            }
        });
    }

    private void loadStoryImage(ImageView imageView, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageView.setImageResource(R.drawable.bg_image_placeholder);
            return;
        }

        String processedUrl = processGoogleDriveUrl(imageUrl);

        Glide.with(context)
                .load(processedUrl)
                .placeholder(R.drawable.bg_image_placeholder)
                .error(R.drawable.bg_image_placeholder)
                .into(imageView);
    }

    private String processGoogleDriveUrl(String imageUrl) {
        if (!imageUrl.contains("drive.google.com")) {
            return imageUrl;
        }

        try {
            if (imageUrl.contains("/d/")) {
                String fileId = imageUrl.split("/d/")[1].split("/")[0];
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            } else if (imageUrl.contains("id=")) {
                String fileId = imageUrl.substring(imageUrl.indexOf("id=") + 3);
                return "https://drive.google.com/uc?export=view&id=" + fileId;
            }
        } catch (Exception e) {
            Log.e("FavoriteAdapter", "Error processing Drive URL: " + e.getMessage());
        }

        return imageUrl;
    }

    private void navigateToReadFragment(FavoriteStory story) {
        Bundle args = new Bundle();
        args.putString("STORY_TITLE", story.getTitle());
        args.putString("STORY_AUTHOR", story.getAuthor());
        args.putString("STORY_ID", story.getStoryId());
        args.putString("STORY_IMAGE_URL", story.getImageUrl());
        args.putString("PDF_LINK", story.getReadUrl());
        args.putString("TAP_TITLE", story.getTitle());
        args.putBoolean("IS_FROM_FAVORITE", true);

        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            ReadFragment readFragment = ReadFragment.newInstance(args);

            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, readFragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            Toast.makeText(context, context.getString(R.string.cannot_open_file), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class FavViewHolder extends RecyclerView.ViewHolder {
        ImageView imageStory;
        TextView textStoryTitle, textAuthor;
        TextView btnRemoveFav;

        public FavViewHolder(@NonNull View itemView) {
            super(itemView);
            imageStory = itemView.findViewById(R.id.imgStory);
            textStoryTitle = itemView.findViewById(R.id.tvStoryTitle);
            textAuthor = itemView.findViewById(R.id.tvAuthor);
            btnRemoveFav = itemView.findViewById(R.id.btnRemoveFav);
        }
    }
}
