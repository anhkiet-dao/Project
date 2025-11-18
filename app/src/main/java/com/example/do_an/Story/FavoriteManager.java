package com.example.do_an.Story;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteManager {

    private static final String TAG = "FavoriteManager";
    private final DatabaseReference database;

    public interface FavoritesCallback {
        void onFavoritesLoaded(List<Map<String, Object>> favorites);
    }

    public FavoriteManager() {
        // ✅ Lưu ở ngoài cùng, không nằm trong "Users"
        database = FirebaseDatabase.getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Favorites");
    }

    // ✅ Lấy danh sách yêu thích theo email
    public void getFavorites(String email, FavoritesCallback callback) {
        String safeEmail = email.replace(".", "_");
        database.child(safeEmail).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Map<String, Object>> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> item = (Map<String, Object>) child.getValue();
                    if (item != null) list.add(item);
                }
                callback.onFavoritesLoaded(list);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Lỗi tải favorites: " + error.getMessage());
                callback.onFavoritesLoaded(new ArrayList<>());
            }
        });
    }

    // ➕ Thêm yêu thích
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

        database.child(safeEmail).child(storyId)
                .setValue(favoriteData)
                .addOnSuccessListener(a -> Log.d(TAG, "✅ Đã thêm yêu thích"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi thêm yêu thích", e));
    }

    // ❌ Xóa yêu thích
    public void removeFavorite(String email, String storyId) {
        String safeEmail = email.replace(".", "_");
        database.child(safeEmail).child(storyId)
                .removeValue()
                .addOnSuccessListener(a -> Log.d(TAG, "🗑️ Đã xóa yêu thích"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi xóa yêu thích", e));
    }
}