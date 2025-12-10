package com.example.do_an.Favorite;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
    private TextView tvNoFavorites, tvTitle;
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

        View view = inflater.inflate(R.layout.favorite_fragment_favorites, container, false);

        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        tvNoFavorites = view.findViewById(R.id.tvNoFavorites);
        tvTitle = view.findViewById(R.id.tvTitle); // 🔥 thêm title

        recyclerFavorites.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⚠️ Kiểm tra đăng nhập
        auth = FirebaseAuth.getInstance();
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
        if (email == null) {
            Toast.makeText(getContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            return view;
        }

        // 🔠 Set text theo string.xml (tự chuyển ngôn ngữ)
        tvTitle.setText(getString(R.string.title_favorite));
        tvNoFavorites.setText(getString(R.string.empty_favorite));

        // ⚙️ Adapter
        adapter = new FavoriteAdapter(getContext(), favoriteList);
        recyclerFavorites.setAdapter(adapter);

        // ❌ Event xóa yêu thích
        adapter.setOnRemoveFavoriteListener((story, position) -> {
            String emailKey = email.replace(".", "_");

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
                checkEmptyState();
                Toast.makeText(getContext(),
                        getString(R.string.remove_favorite_success, story.getTitle()),
                        Toast.LENGTH_SHORT
                ).show();
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        // 🔗 Firebase Reference
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
                        story.setStoryId(storySnap.getKey());
                        favoriteList.add(story);
                    }
                }
                adapter.notifyDataSetChanged();
                checkEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(),
                        getString(R.string.favorite_error),
                        Toast.LENGTH_LONG).show();
            }
        };

        favRef.addValueEventListener(favEventListener);
    }

    private void checkEmptyState() {
        if (favoriteList.isEmpty()) {
            recyclerFavorites.setVisibility(View.GONE);
            tvNoFavorites.setVisibility(View.VISIBLE);
        } else {
            recyclerFavorites.setVisibility(View.VISIBLE);
            tvNoFavorites.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (favRef != null && favEventListener != null) {
            favRef.removeEventListener(favEventListener);
        }
    }
}
