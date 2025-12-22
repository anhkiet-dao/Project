package com.example.do_an.presentation.library.favorites;

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
import com.example.do_an.core.constant.FirebaseConstants;
import com.example.do_an.domain.library.model.FavoriteStory;
import com.example.do_an.presentation.library.favorites.adapter.FavoriteAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerFavorites;
    private TextView textNoFavorites;
    private FavoriteAdapter adapter;
    private final List<FavoriteStory> favoriteList = new ArrayList<>();
    private DatabaseReference favRef;
    private FirebaseAuth auth;
    private ValueEventListener favEventListener;
    private boolean isViewCreated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.favorite_fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        auth = FirebaseAuth.getInstance();

        if (!isUserLoggedIn()) {
            showNotLoggedInMessage();
            return;
        }

        setupViews(view);
        loadViews();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
        removeFirebaseListener();
    }

    private boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null && auth.getCurrentUser().getEmail() != null;
    }

    private void showNotLoggedInMessage() {
        Toast.makeText(getContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
    }

    private void setupViews(View view) {
        recyclerFavorites = view.findViewById(R.id.recyclerFavorites);
        textNoFavorites = view.findViewById(R.id.tvNoFavorites);

        recyclerFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        textNoFavorites.setText(getString(R.string.empty_favorite));

        adapter = new FavoriteAdapter(getContext(), favoriteList);
        recyclerFavorites.setAdapter(adapter);
    }

    private void loadViews() {
        String emailKey = getEmailKey();
        favRef = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
                .getReference("Favorites")
                .child(emailKey);

        fetchAndDisplayFavorites();
    }

    private void setupListeners() {
        adapter.setOnRemoveFavoriteListener(this::handleRemoveFavorite);
    }

    private String getEmailKey() {
        return auth.getCurrentUser().getEmail().replace(".", "_");
    }

    private void handleRemoveFavorite(FavoriteStory story, int position) {
        if (!isViewCreated || getContext() == null)
            return;

        String emailKey = getEmailKey();
        DatabaseReference ref = FirebaseDatabase.getInstance(FirebaseConstants.DATABASE_URL)
                .getReference("Favorites")
                .child(emailKey)
                .child(story.getStoryId());

        ref.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (isViewCreated) {
                        removeFavoriteFromList(story, position);
                    }
                })
                .addOnFailureListener(e -> {
                    if (isViewCreated && getContext() != null) {
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void removeFavoriteFromList(FavoriteStory story, int position) {
        if (position >= 0 && position < favoriteList.size()) {
            favoriteList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, favoriteList.size());
        }
        updateEmptyState();
        showRemoveSuccessMessage(story.getTitle());
    }

    private void showRemoveSuccessMessage(String title) {
        if (getContext() != null) {
            Toast.makeText(getContext(),
                    getString(R.string.remove_favorite_success, title),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchAndDisplayFavorites() {
        if (!isViewCreated)
            return;

        removeFirebaseListener();

        favEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isViewCreated)
                    return;
                processFavoriteData(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isViewCreated || getContext() == null)
                    return;
                showLoadErrorMessage();
            }
        };

        favRef.addValueEventListener(favEventListener);
    }

    private void processFavoriteData(DataSnapshot snapshot) {
        favoriteList.clear();

        for (DataSnapshot storySnap : snapshot.getChildren()) {
            FavoriteStory story = storySnap.getValue(FavoriteStory.class);
            if (story != null) {
                story.setStoryId(storySnap.getKey());
                favoriteList.add(story);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showLoadErrorMessage() {
        Toast.makeText(getContext(),
                getString(R.string.favorite_error),
                Toast.LENGTH_LONG).show();
    }

    private void updateEmptyState() {
        if (!isViewCreated)
            return;

        if (favoriteList.isEmpty()) {
            showEmptyState();
        } else {
            showFavoritesList();
        }
    }

    private void showEmptyState() {
        recyclerFavorites.setVisibility(View.GONE);
        textNoFavorites.setVisibility(View.VISIBLE);
    }

    private void showFavoritesList() {
        recyclerFavorites.setVisibility(View.VISIBLE);
        textNoFavorites.setVisibility(View.GONE);
    }

    private void removeFirebaseListener() {
        if (favRef != null && favEventListener != null) {
            favRef.removeEventListener(favEventListener);
        }
    }
}
