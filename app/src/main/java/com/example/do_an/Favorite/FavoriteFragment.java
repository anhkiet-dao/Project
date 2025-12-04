package com.example.do_an.Favorite;

import android.os.Bundle;
import android.util.Log;
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
    private ValueEventListener favEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.favorite_activity_favorites, container, false);

        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        recyclerFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        auth = FirebaseAuth.getInstance();
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
        if (email == null) {
            Toast.makeText(getContext(), "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return view;
        }

        adapter = new FavoriteAdapter(getContext(), favoriteList);
        recyclerFavorites.setAdapter(adapter);

        adapter.setOnRemoveFavoriteListener((story, position) -> {
            String emailKey = email.replace(".", "_");

            Log.d("FavoriteFragment", "Đang xóa truyện ID: " + story.getStoryId()
                    + " tại path: Favorites/" + emailKey + "/" + story.getStoryId());

            DatabaseReference ref = FirebaseDatabase
                    .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
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
                    Toast.makeText(getContext(), "Lỗi khi bỏ yêu thích: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        String emailKey = email.replace(".", "_");
        favRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Favorites")
                .child(emailKey);

        loadFavoriteStories();
        return view;
    }

    private void loadFavoriteStories() {
        if (favEventListener != null) {
            favRef.removeEventListener(favEventListener);
        }

        favEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                favoriteList.clear();
                for (DataSnapshot storySnap : snapshot.getChildren()) {
                    FavoriteStory story = storySnap.getValue(FavoriteStory.class);

                    if (story != null) {

                        String firebaseKey = storySnap.getKey();
                        story.setStoryId(firebaseKey);

                        try {
                            if (story.getTitle() != null) {
                                story.setTitle((story.getTitle()));
                            }
                            if (story.getAuthor() != null) {
                                story.setAuthor((story.getAuthor()));
                            }
                            if (story.getCategory() != null) {
                                story.setCategory((story.getCategory()));
                            }
                            if (story.getImageUrl() != null) {
                                story.setImageUrl((story.getImageUrl()));
                            }
                            if (story.getReadUrl() != null) {
                                story.setReadUrl((story.getReadUrl()));
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        favoriteList.add(story);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải danh sách yêu thích: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        favRef.addValueEventListener(favEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (favRef != null && favEventListener != null) {
            favRef.removeEventListener(favEventListener);
        }
    }
}