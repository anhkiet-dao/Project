package com.example.do_an.presentation.common;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.core.utils.Encryption;
import com.example.do_an.presentation.library.downloads.DownloadFragment;
import com.example.do_an.presentation.library.favorites.FavoritesFragment;
import com.example.do_an.presentation.library.history.HistoryFragment;
import com.example.do_an.presentation.profile.statistics.StatisticFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class MyListFragment extends Fragment {

    private Button btnAnalytics, btnHistory, btnFavorite, btnDownload;
    private TextView textGreeting;
    private boolean isViewCreated = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_activity_mylist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        setupViews(view);
        loadViews();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    private void setupViews(View view) {
        btnAnalytics = view.findViewById(R.id.btnAnalytics);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnDownload = view.findViewById(R.id.btnDownload);
        textGreeting = view.findViewById(R.id.textGreeting);

        refreshButtonTexts();
    }

    private void refreshButtonTexts() {
        btnAnalytics.setText(getString(R.string.analytics));
        btnHistory.setText(getString(R.string.history));
        btnFavorite.setText(getString(R.string.favorite));
        btnDownload.setText(getString(R.string.download));
    }

    private void loadViews() {
        displayUserGreeting();
        loadFragment(new DownloadFragment());
        selectButton(btnDownload);
    }

    private void setupListeners() {
        btnAnalytics.setOnClickListener(v -> {
            loadFragment(new StatisticFragment());
            selectButton(btnAnalytics);
        });
        btnHistory.setOnClickListener(v -> {
            loadFragment(new HistoryFragment());
            selectButton(btnHistory);
        });
        btnFavorite.setOnClickListener(v -> {
            loadFragment(new FavoritesFragment());
            selectButton(btnFavorite);
        });
        btnDownload.setOnClickListener(v -> {
            loadFragment(new DownloadFragment());
            selectButton(btnDownload);
        });
    }

    private void selectButton(Button selected) {
        for (Button btn : new Button[]{btnAnalytics, btnHistory, btnFavorite, btnDownload}) {
            btn.setSelected(btn == selected);
        }
    }

    private void loadFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.containerContent, fragment)
                .commit();
    }

    private void displayUserGreeting() {
        if (textGreeting == null)
            return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            textGreeting.setText(getString(R.string.hello_user));
            return;
        }

        fetchUserName(currentUser.getEmail());
    }

    private void fetchUserName(String userEmail) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isViewCreated)
                    return;
                String userName = findUserNameByEmail(snapshot, userEmail);
                displayGreetingWithName(userName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isViewCreated)
                    return;
                textGreeting.setText(getString(R.string.hello_user));
            }
        });
    }

    private String findUserNameByEmail(DataSnapshot snapshot, String targetEmail) {
        for (DataSnapshot userSnap : snapshot.getChildren()) {
            String encryptedEmail = userSnap.child("email").getValue(String.class);
            if (encryptedEmail == null)
                continue;

            try {
                String decryptedEmail = Encryption.decrypt(encryptedEmail.trim());
                if (targetEmail.equals(decryptedEmail)) {
                    return extractUserName(userSnap);
                }
            } catch (Exception ignored) {
            }
        }
        return getString(R.string.default_user);
    }

    private String extractUserName(DataSnapshot userSnap) {
        String encryptedName = userSnap.child("fullName").getValue(String.class);
        if (encryptedName != null && !encryptedName.isEmpty()) {
            try {
                return Encryption.decrypt(encryptedName.trim());
            } catch (Exception ignored) {
            }
        }
        return getString(R.string.default_user);
    }

    @SuppressLint("SetTextI18n")
    private void displayGreetingWithName(String userName) {
        String greeting = getGreetingByTime();
        textGreeting.setText(greeting + " " + userName + "!");
    }

    private String getGreetingByTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11)
            return getString(R.string.good_morning);
        if (hour < 13)
            return getString(R.string.good_noon);
        if (hour < 18)
            return getString(R.string.good_afternoon);
        return getString(R.string.good_evening);
    }
}
