package com.example.do_an.library.favorite;

import android.content.Context;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.do_an.R;

import java.util.Map;

public class FavoriteHandler {
    private final Context context;
    private final FavoriteManager favoriteManager = new FavoriteManager();

    public FavoriteHandler(Context context) {
        this.context = context;
    }

    private String getFavoriteTitle(String mainStoryTitle, String currentTitle) {
        // Nếu tên hiển thị (currentTitle) khác tên truyện chính, coi nó là tên tập
        if (mainStoryTitle != null && !mainStoryTitle.equals(currentTitle)) {
            return mainStoryTitle + " - " + currentTitle;
        }
        return (mainStoryTitle != null) ? mainStoryTitle : currentTitle;
    }

    public void checkIfFavorite(String storyId, String mainTitle, String currentTitle, String userEmail, ImageView btnFavorite) {
        if (userEmail == null || storyId == null) return;

        final String titleToCheck = getFavoriteTitle(mainTitle, currentTitle);

        favoriteManager.getFavorites(userEmail, favorites -> {
            boolean isFavorite = false;
            for (Map<String, Object> item : favorites) {
                if (item != null) {
                    String id = (String) item.get("storyId");
                    String title = (String) item.get("title");

                    if (storyId.equals(id) && titleToCheck.equals(title)) {
                        isFavorite = true;
                        break;
                    }
                }
            }

            // Lưu trạng thái và đặt icon
            btnFavorite.setTag(isFavorite);
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            }
        });
    }

    public void toggleFavorite(String userEmail, String storyId, String mainTitle, String currentTitle,
                               String author, String category, String imageUrl, String readUrl,
                               ImageView btnFavorite) {
        if (userEmail == null || storyId == null || readUrl == null) {
            Toast.makeText(context, "Lỗi: Không đủ thông tin để lưu yêu thích.", Toast.LENGTH_SHORT).show();
            return;
        }

        final String titleForFavorite = getFavoriteTitle(mainTitle, currentTitle);

        boolean isFavorite = btnFavorite.getTag() != null && (boolean) btnFavorite.getTag();

        if (!isFavorite) {
            // Thêm yêu thích
            favoriteManager.addFavorite(
                    userEmail, storyId, titleForFavorite, author, category, null, imageUrl, readUrl
            );
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            btnFavorite.setTag(true);
            Toast.makeText(context, "Đã thêm: " + titleForFavorite + " vào yêu thích ❤️", Toast.LENGTH_SHORT).show();
        } else {
            // Xóa yêu thích
            favoriteManager.removeFavorite(userEmail, storyId, titleForFavorite);
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            btnFavorite.setTag(false);
            Toast.makeText(context, "Đã xóa khỏi yêu thích 💔", Toast.LENGTH_SHORT).show();
        }
    }
}