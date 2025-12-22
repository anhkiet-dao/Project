package com.example.do_an.presentation.reading.reader;

import android.content.Context;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.do_an.R;
import com.example.do_an.domain.reading.repository.FavoriteRepository;

import java.util.Locale;
import java.util.Map;

public class FavoriteHandler {
    private final Context context;
    private final FavoriteRepository favoriteRepository;

    public FavoriteHandler(Context context) {
        this.context = context;
        favoriteRepository = new FavoriteRepository(context);
    }

    /** Chọn ngôn ngữ hiển thị (Toast/Log) */
    public void setLocale(Locale locale) {
        favoriteRepository.setLocale(locale);
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

        favoriteRepository.getFavorites(userEmail, favorites -> {
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

            // SỬA TẠI ĐÂY: Đưa vào Luồng Chính để cập nhật UI
            final boolean finalIsFavorite = isFavorite;
            btnFavorite.post(() -> {
                btnFavorite.setTag(finalIsFavorite);
                btnFavorite.setImageResource(finalIsFavorite
                        ? R.drawable.ic_favorite_filled
                        : R.drawable.ic_favorite_border);
                btnFavorite.invalidate(); // Buộc view vẽ lại
            });
        });
    }

    public void toggleFavorite(String userEmail, String storyId, String mainTitle, String currentTitle,
                               String author, String category, String imageUrl, String readUrl,
                               ImageView btnFavorite) {
        if (userEmail == null || storyId == null || readUrl == null) {
            Toast.makeText(context, "Thiếu thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        final String titleForFavorite = getFavoriteTitle(mainTitle, currentTitle);

        // Lấy trạng thái hiện tại từ Tag
        Object tagValue = btnFavorite.getTag();
        boolean isFavorite = (tagValue instanceof Boolean) && (Boolean) tagValue;

        if (!isFavorite) {
            // Luồng logic: Thêm vào Firebase
            favoriteRepository.addFavorite(userEmail, storyId, titleForFavorite, author, category, null, imageUrl, readUrl);

            // Cập nhật UI ngay lập tức trên Main Thread
            btnFavorite.post(() -> {
                btnFavorite.setTag(true); // Gán Tag trước
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                btnFavorite.invalidate();
            });
        } else {
            // Luồng logic: Xóa khỏi Firebase
            favoriteRepository.removeFavorite(userEmail, storyId, titleForFavorite);

            // Cập nhật UI ngay lập tức trên Main Thread
            btnFavorite.post(() -> {
                btnFavorite.setTag(false); // Gán Tag trước
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                btnFavorite.invalidate();
            });
        }
    }
}
