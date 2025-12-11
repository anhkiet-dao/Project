package com.example.do_an.Favorite;

import android.content.Context;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.do_an.R;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoriteHandler {
    private final Context context;
    private final FavoriteManager favoriteManager;

    public FavoriteHandler(Context context) {
        this.context = context;
        favoriteManager = new FavoriteManager(context);
    }

    /** Chọn ngôn ngữ hiển thị (Toast/Log) */
    public void setLocale(Locale locale) {
        favoriteManager.setLocale(locale);
    }

    private String getFavoriteTitle(String mainStoryTitle, String currentTitle) {
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

            btnFavorite.setTag(isFavorite);
            btnFavorite.setImageResource(isFavorite
                    ? R.drawable.ic_favorite_filled
                    : R.drawable.ic_favorite_border);
        });
    }

    public void toggleFavorite(String userEmail, String storyId, String mainTitle, String currentTitle,
                               String author, String category, String imageUrl, String readUrl,
                               ImageView btnFavorite) {
        if (userEmail == null || storyId == null || readUrl == null) {
            Toast.makeText(context, context.getString(R.string.error_insufficient_info), Toast.LENGTH_SHORT).show();
            return;
        }

        final String titleForFavorite = getFavoriteTitle(mainTitle, currentTitle);
        boolean isFavorite = btnFavorite.getTag() != null && (boolean) btnFavorite.getTag();

        if (!isFavorite) {
            favoriteManager.addFavorite(userEmail, storyId, titleForFavorite, author, category, null, imageUrl, readUrl);
            btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
            btnFavorite.setTag(true);
        } else {
            favoriteManager.removeFavorite(userEmail, storyId, titleForFavorite);
            btnFavorite.setImageResource(R.drawable.ic_favorite_border);
            btnFavorite.setTag(false);
        }
    }
}
