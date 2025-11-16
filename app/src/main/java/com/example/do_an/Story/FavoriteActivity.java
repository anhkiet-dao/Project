package com.example.do_an.Story;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private RecyclerView recyclerFavorites;
    private FavoriteAdapter adapter;
    private final List<FavoriteStory> favoriteList = new ArrayList<>();

    private DatabaseReference favRef;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        recyclerFavorites = findViewById(R.id.recyclerFavorites);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));

        auth = FirebaseAuth.getInstance();
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
        if (email == null) {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khởi tạo adapter 1 lần
        adapter = new FavoriteAdapter(this, favoriteList);
        recyclerFavorites.setAdapter(adapter);

        // Xử lý nút bỏ yêu thích
        adapter.setOnRemoveFavoriteListener((story, position) -> {
            String emailKey = email.replace(".", "_");
            DatabaseReference ref = FirebaseDatabase
                    .getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("Favorites")
                    .child(emailKey)
                    .child(story.getStoryId());

            ref.removeValue().addOnSuccessListener(aVoid -> {
                // Xóa cục bộ, không reload danh sách
                if (position >= 0 && position < favoriteList.size()) {
                    favoriteList.remove(position);
                    adapter.notifyItemRemoved(position);
                }
                adapter.notifyItemRangeChanged(position, favoriteList.size());
                Toast.makeText(FavoriteActivity.this, "Đã bỏ yêu thích: " + story.getTitle(), Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(FavoriteActivity.this, "Lỗi khi bỏ yêu thích", Toast.LENGTH_SHORT).show();
            });
        });

        // Chỉ load dữ liệu 1 lần, không dùng listener liên tục
        String emailKey = email.replace(".", "_");
        favRef = FirebaseDatabase
                .getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Favorites")
                .child(emailKey);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteList.clear();
                for (DataSnapshot storySnap : snapshot.getChildren()) {
                    FavoriteStory story = storySnap.getValue(FavoriteStory.class);
                    if (story != null) favoriteList.add(story);
                }
                adapter.notifyDataSetChanged(); // cập nhật UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }
}
