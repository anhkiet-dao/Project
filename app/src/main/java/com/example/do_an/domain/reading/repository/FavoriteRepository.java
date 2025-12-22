package com.example.do_an.domain.reading.repository;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;

import com.example.do_an.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FavoriteRepository {

    private static final String TAG = "FavoriteManager";
    private final DatabaseReference database;
    private final Context context;
    private Locale currentLocale = Locale.getDefault(); // Ngôn ngữ mặc định

    public interface FavoritesCallback {
        void onFavoritesLoaded(List<Map<String, Object>> favorites);
    }

    public FavoriteRepository(Context context) {
        this.context = context;
        database = FirebaseDatabase.getInstance(
                "https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("Favorites");
    }

    /** Set ngôn ngữ runtime cho Toast/Log */
    public void setLocale(Locale locale) {
        this.currentLocale = locale;
    }

    private String getString(int resId) {
        return context.createConfigurationContext(context.getResources().getConfiguration())
                .getResources().getString(resId);
    }

    private String getStringByLocale(int resId, Locale locale) {
        return context.createConfigurationContext(context.getResources().getConfiguration())
                .getResources().getString(resId);
    }

    public void getFavorites(String email, FavoritesCallback callback) {
        String safeEmail = email.replace(".", "_");
        database.child(safeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> item = (Map<String, Object>) child.getValue();
                    if (item != null) list.add(item);
                }
                callback.onFavoritesLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String msg = getStringByLocale(R.string.error_load_favorites, currentLocale)
                        + ": " + error.getMessage();
                Log.e(TAG, msg);
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                callback.onFavoritesLoaded(new ArrayList<>());
            }
        });
    }

    public void addFavorite(String email, String storyId, String title, String author, String category,
                            String description, String imageUrl, String readUrl) {
        String safeEmail = email.replace(".", "_");

        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("storyId", storyId);
        favoriteData.put("title", title);
        favoriteData.put("author", author);
        favoriteData.put("category", category);
        favoriteData.put("imageUrl", imageUrl);
        favoriteData.put("readUrl", readUrl);

        database.child(safeEmail).push()
                .setValue(favoriteData)
                .addOnSuccessListener(a -> {
                    String msg = getStringByLocale(R.string.add_favorite_success, currentLocale) + ": " + title;
                    Log.d(TAG, msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    String msg = getStringByLocale(R.string.add_favorite_fail, currentLocale);
                    Log.e(TAG, msg, e);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                });
    }

    public void removeFavorite(String email, String storyId, String titleToRemove) {
        String safeEmail = email.replace(".", "_");

        Query query = database.child(safeEmail).orderByChild("storyId").equalTo(storyId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    String title = itemSnapshot.child("title").getValue(String.class);
                    if (title != null && title.equals(titleToRemove)) {
                        itemSnapshot.getRef().removeValue()
                                .addOnSuccessListener(a -> {
                                    String msg = getStringByLocale(R.string.remove_favorite_success1, currentLocale)
                                            + ": " + titleToRemove;
                                    Log.d(TAG, msg);
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    String msg = getStringByLocale(R.string.remove_favorite_fail, currentLocale);
                                    Log.e(TAG, msg, e);
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                String msg = getStringByLocale(R.string.error_load_favorites, currentLocale)
                        + ": " + error.getMessage();
                Log.e(TAG, msg, error.toException());
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
