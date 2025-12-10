package com.example.do_an.UI;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an.Download.DownloadFragment;
import com.example.do_an.Favorite.FavoriteFragment;
import com.example.do_an.History.HistoryFragment;
import com.example.do_an.R;
import com.example.do_an.Statistic.StatisticFragment;
import com.example.do_an.application.Encryption;
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
    private TextView tvGreeting;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ui_activity_mylist, container, false);

        btnAnalytics = view.findViewById(R.id.btnAnalytics);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnDownload = view.findViewById(R.id.btnDownload);
        tvGreeting = view.findViewById(R.id.txtGreeting);

        // Set text từ strings.xml
        btnAnalytics.setText(getString(R.string.analytics));
        btnHistory.setText(getString(R.string.history));
        btnFavorite.setText(getString(R.string.favorite));
        btnDownload.setText(getString(R.string.download));

        setupUserGreeting();

        btnAnalytics.setOnClickListener(v -> {
            loadFragment(new StatisticFragment());
            selectButton(btnAnalytics);
        });
        btnHistory.setOnClickListener(v -> {
            loadFragment(new HistoryFragment());
            selectButton(btnHistory);
        });
        btnFavorite.setOnClickListener(v -> {
            loadFragment(new FavoriteFragment());
            selectButton(btnFavorite);
        });
        btnDownload.setOnClickListener(v -> {
            loadFragment(new DownloadFragment());
            selectButton(btnDownload);
        });

        loadFragment(new DownloadFragment());
        selectButton(btnDownload);

        return view;
    }

    private void selectButton(Button selected) {
        btnAnalytics.setBackgroundResource(R.drawable.button_unselected);
        btnHistory.setBackgroundResource(R.drawable.button_unselected);
        btnFavorite.setBackgroundResource(R.drawable.button_unselected);
        btnDownload.setBackgroundResource(R.drawable.button_unselected);

        btnAnalytics.setTextColor(getResources().getColor(android.R.color.white));
        btnHistory.setTextColor(getResources().getColor(android.R.color.white));
        btnFavorite.setTextColor(getResources().getColor(android.R.color.white));
        btnDownload.setTextColor(getResources().getColor(android.R.color.white));

        selected.setBackgroundResource(R.drawable.button_selected);
        selected.setTextColor(getResources().getColor(android.R.color.holo_blue_bright));
    }

    private void loadFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.containerContent, fragment)
                .commit();
    }

    // ================== LỜI CHÀO ĐA NGÔN NGỮ =====================
    private void setupUserGreeting() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            tvGreeting.setText(getString(R.string.hello_user));
            return;
        }

        final String userEmail = currentUser.getEmail();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String encryptedEmail = userSnap.child("email").getValue(String.class);
                    if (encryptedEmail == null) continue;

                    try {
                        String emailDecrypted = Encryption.decrypt(encryptedEmail.trim());
                        if (userEmail.equals(emailDecrypted)) {
                            String encryptedName = userSnap.child("fullName").getValue(String.class);
                            String realName = getString(R.string.default_user);

                            if (encryptedName != null && !encryptedName.isEmpty()) {
                                realName = Encryption.decrypt(encryptedName.trim());
                            }

                            Calendar calendar = Calendar.getInstance();
                            int hour = calendar.get(Calendar.HOUR_OF_DAY);
                            String greeting;
                            if (hour < 11) greeting = getString(R.string.good_morning);
                            else if (hour < 13) greeting = getString(R.string.good_noon);
                            else if (hour < 18) greeting = getString(R.string.good_afternoon);
                            else greeting = getString(R.string.good_evening);

                            tvGreeting.setText(greeting + " " + realName + "!");
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                tvGreeting.setText(getString(R.string.hello_user));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvGreeting.setText(getString(R.string.hello_user));
            }
        });
    }
}
