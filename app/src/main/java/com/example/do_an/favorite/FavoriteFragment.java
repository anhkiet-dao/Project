package com.example.do_an.favorite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private RecyclerView recyclerFavorites;
    private FavoriteAdapter adapter;
    private final List<FavoriteStory> favoriteList = new ArrayList<>();

    private DatabaseReference favRef;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.favorite_activity_favorites, container, false); // ⚠️ Đảm bảo bạn có file XML fragment_favorite.xml

        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        auth = FirebaseAuth.getInstance();
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
        if (email == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Khởi tạo adapter 1 lần
        adapter = new FavoriteAdapter(getContext(), favoriteList);
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
                if (position >= 0 && position < favoriteList.size()) {
                    favoriteList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, favoriteList.size());
                }
                Toast.makeText(getContext(), "Đã bỏ yêu thích: " + story.getTitle(), Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Lỗi khi bỏ yêu thích", Toast.LENGTH_SHORT).show()
            );
        });

        // Chỉ load dữ liệu 1 lần, không lắng nghe liên tục
        String emailKey = email.replace(".", "_");
        favRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Favorites")
                .child(emailKey);

        favRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteList.clear();
                for (DataSnapshot storySnap : snapshot.getChildren()) {
                    FavoriteStory story = storySnap.getValue(FavoriteStory.class);
                    if (story != null) {
                        favoriteList.add(story);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}
