package com.example.do_an.Favorite;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.do_an.R;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.FavViewHolder> {

    private final Context context;
    private final List<FavoriteStory> favoriteList;

    public FavoriteAdapter(Context context, List<FavoriteStory> favoriteList) {
        this.context = context;
        this.favoriteList = favoriteList;
    }

    public interface OnRemoveFavoriteListener {
        void onRemoveFavorite(FavoriteStory story, int position);
    }

    private OnRemoveFavoriteListener removeListener;

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

        holder.tvStoryTitle.setText(story.getTitle());
        holder.tvAuthor.setText("Tác giả: " + story.getAuthor());

        String imageUrl = story.getImageUrl();
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            holder.imgStory.setImageResource(R.drawable.bg_image_placeholder);
        } else {
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
                    Log.e("FavoriteAdapter", "Lỗi tách ID Drive: " + e.getMessage());
                }
            }

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.bg_image_placeholder)
                    .error(R.drawable.bg_image_placeholder)
                    .into(holder.imgStory);
        }

        // Xử lý nút Bỏ yêu thích
        holder.btnRemoveFav.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemoveFavorite(story, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return favoriteList.size();
    }

    public static class FavViewHolder extends RecyclerView.ViewHolder {
        ImageView imgStory;
        TextView tvStoryTitle, tvAuthor, tvCategory, tvDescription;
        Button btnRemoveFav;

        public FavViewHolder(@NonNull View itemView) {
            super(itemView);
            imgStory = itemView.findViewById(R.id.imgStory);
            tvStoryTitle = itemView.findViewById(R.id.tvStoryTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            btnRemoveFav = itemView.findViewById(R.id.btnRemoveFav);
        }
    }
}
