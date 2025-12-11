package com.example.do_an.Series;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.do_an.R;
import com.example.do_an.UI.ReadFragment;
import com.example.do_an.application.Encryption;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SeriesFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvNoSeries;
    private TextView txtGreeting;
    private TextView toolbarTitle;
    private SeriesAdapter adapter;
    private final List<Series> seriesList = new ArrayList<>();
    private FirebaseFirestore db;
    private String storyId, storyName, storyAuthor, storyCategory, storyDescription, storyImageUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.series_activity_series, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtGreeting = view.findViewById(R.id.txtGreeting);
        toolbarTitle = view.findViewById(R.id.toolbar_title);

        setupUserGreeting();

        Bundle args = getArguments();
        if (args != null) {
            storyId = args.getString("STORY_ID");
            storyName = args.getString("STORY_NAME");
            storyAuthor = args.getString("STORY_AUTHOR");
            storyCategory = args.getString("STORY_CATEGORY");
            storyDescription = args.getString("STORY_DESCRIPTION");
            storyImageUrl = args.getString("STORY_IMAGE_URL");
        }

        if (storyId == null || storyId.isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.error_story_not_found), Toast.LENGTH_SHORT).show();
            }
            if (getActivity() != null) getActivity().onBackPressed();
            return;
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbarTitle != null) {
            toolbarTitle.setText(storyName != null && !storyName.isEmpty() ? storyName : getString(R.string.series_list));
        } else {
            toolbar.setTitle(storyName != null && !storyName.isEmpty() ? storyName : getString(R.string.series_list));
        }

        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        recyclerView = view.findViewById(R.id.recyclerSeries);
        tvNoSeries = view.findViewById(R.id.tvNoSeries);
        int numberOfColumns = 3;

        if (getContext() != null) {
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
            adapter = new SeriesAdapter(seriesList, series -> {
                if (getContext() == null || getActivity() == null) return;

                Bundle readArgs = new Bundle();
                readArgs.putString("STORY_ID", storyId);
                readArgs.putString("STORY_TITLE", storyName);
                readArgs.putString("STORY_AUTHOR", storyAuthor);
                readArgs.putString("STORY_CATEGORY", storyCategory);
                readArgs.putString("STORY_DESCRIPTION", storyDescription);
                readArgs.putString("STORY_IMAGE_URL", storyImageUrl);
                readArgs.putString("PDF_LINK", series.getLink());
                readArgs.putString("TAP", series.getName());

                ReadFragment readFragment = new ReadFragment();
                readFragment.setArguments(readArgs);

                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .add(R.id.fragment_container, readFragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        }

        db = FirebaseFirestore.getInstance();
        loadSeries();
    }

    private void setupUserGreeting() {
        if (txtGreeting == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            txtGreeting.setText(getString(R.string.hello_user));
            return;
        }

        final String userEmail = currentUser.getEmail();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                String realName = getString(R.string.default_user_name);

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String encryptedEmail = userSnap.child("email").getValue(String.class);
                    if (encryptedEmail == null) continue;

                    try {
                        String emailDecrypted = Encryption.decrypt(encryptedEmail.trim());
                        if (userEmail.equals(emailDecrypted)) {
                            String encryptedName = userSnap.child("fullName").getValue(String.class);
                            if (encryptedName != null && !encryptedName.isEmpty()) {
                                realName = Encryption.decrypt(encryptedName.trim());
                            }
                            break;
                        }
                    } catch (Exception e) {
                        Log.e("SeriesFragment", "Lỗi giải mã email/tên: " + e.getMessage());
                    }
                }

                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                String greeting;
                if (hour < 11) greeting = getString(R.string.greeting_morning);
                else if (hour < 13) greeting = getString(R.string.greeting_noon);
                else if (hour < 18) greeting = getString(R.string.greeting_afternoon);
                else greeting = getString(R.string.greeting_evening);

                txtGreeting.setText(greeting + ", " + realName + "!");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isAdded() || getContext() == null) return;
                txtGreeting.setText(getString(R.string.hello_user));
            }
        });
    }

    private void loadSeries() {
        if (storyId == null || db == null) return;

        db.collection("story")
                .document(storyId)
                .collection("Series")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (getContext() == null || !isAdded()) return;

                    seriesList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Series s = doc.toObject(Series.class);
                        s.setId(doc.getId());
                        if (s.getName() != null && s.getLink() != null) {
                            seriesList.add(s);
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();

                    if (seriesList.isEmpty()) {
                        tvNoSeries.setText(getString(R.string.no_series));
                        tvNoSeries.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvNoSeries.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null && isAdded()) {
                        if (seriesList.isEmpty()) {
                            tvNoSeries.setText(getString(R.string.error_loading_data));
                            tvNoSeries.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(getContext(), getString(R.string.error_loading_data), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /** Hàm chuyển đổi ngôn ngữ runtime */
    public void switchLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Reload fragment để cập nhật text
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
        }
    }
}
