package com.example.do_an.Story;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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
        database = FirebaseDatabase.getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Favorites");
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
                Log.e(TAG, "Lỗi tải favorites: " + error.getMessage());
                callback.onFavoritesLoaded(new ArrayList<>());
            }
        });
    }

    // ✅ SỬA: Dùng push() để tạo key ngẫu nhiên, không bị trùng
    public void addFavorite(String email, String storyId, String title, String author, String category,
                            String description, String imageUrl, String readUrl) {
        String safeEmail = email.replace(".", "_");

        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("storyId", storyId);
        favoriteData.put("title", title); // Lưu cả tên tập để phân biệt
        favoriteData.put("author", author);
        favoriteData.put("category", category);
        favoriteData.put("imageUrl", imageUrl);
        favoriteData.put("readUrl", readUrl);

        // Thay vì child(storyId), ta dùng push() để tạo key mới mỗi lần lưu
        database.child(safeEmail).push()
                .setValue(favoriteData)
                .addOnSuccessListener(a -> Log.d(TAG, "✅ Đã thêm yêu thích: " + title))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi thêm yêu thích", e));
    }

    // ✅ SỬA: Vì dùng push(), ta không biết key là gì để xóa ngay.
    // Ta phải tìm node có storyId VÀ title khớp với cái muốn xóa.
    public void removeFavorite(String email, String storyId, String titleToRemove) {
        String safeEmail = email.replace(".", "_");

        // Query tìm các mục trong danh sách của user
        Query query = database.child(safeEmail).orderByChild("storyId").equalTo(storyId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot appleSnapshot: snapshot.getChildren()) {
                    // Lấy data ra kiểm tra
                    String title = appleSnapshot.child("title").getValue(String.class);

                    // Nếu storyId khớp (do query) VÀ title khớp -> Xóa đúng cái đó
                    if (title != null && title.equals(titleToRemove)) {
                        appleSnapshot.getRef().removeValue()
                                .addOnSuccessListener(a -> Log.d(TAG, "🗑️ Đã xóa: " + titleToRemove));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }
}