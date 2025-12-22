package com.example.do_an.presentation.library.series;

import android.annotation.SuppressLint;
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
import com.example.do_an.core.utils.Encryption;
import com.example.do_an.domain.library.model.Series;
import com.example.do_an.presentation.library.series.adapter.SeriesAdapter;
import com.example.do_an.presentation.reading.reader.ReadFragment;
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

public class SeriesFragment extends Fragment {

    private RecyclerView recyclerSeries;
    private TextView textNoSeries;
    private TextView textGreeting;
    private TextView textToolbarTitle;
    private MaterialToolbar toolbar;

    private SeriesAdapter adapter;
    private final List<Series> seriesList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private String storyId, storyName, storyAuthor, storyCategory, storyDescription, storyImageUrl;
    private boolean isViewCreated = false;

    public static SeriesFragment newInstance(Bundle args) {
        SeriesFragment fragment = new SeriesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.series_activity_series, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        firestore = FirebaseFirestore.getInstance();

        if (!checkArguments())
            return;

        setupViews(view);
        loadViews();
        setupListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isViewCreated = false;
    }

    private boolean checkArguments() {
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
            if (getActivity() != null)
                getActivity().onBackPressed();
            return false;
        }
        return true;
    }

    private void setupViews(View view) {
        textGreeting = view.findViewById(R.id.textGreeting);
        textToolbarTitle = view.findViewById(R.id.textToolbarTitle);
        toolbar = view.findViewById(R.id.toolbar);
        recyclerSeries = view.findViewById(R.id.recyclerSeries);
        textNoSeries = view.findViewById(R.id.textNoSeries);

        setupToolbar();
        setupRecyclerView();
    }

    private void setupToolbar() {
        if (textToolbarTitle != null) {
            textToolbarTitle
                    .setText(storyName != null && !storyName.isEmpty() ? storyName : getString(R.string.series_list));
        } else if (toolbar != null) {
            toolbar.setTitle(storyName != null && !storyName.isEmpty() ? storyName : getString(R.string.series_list));
        }

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null)
                    getActivity().onBackPressed();
            });
        }
    }

    private void setupRecyclerView() {
        if (getContext() == null)
            return;

        int numberOfColumns = 3;
        recyclerSeries.setLayoutManager(new GridLayoutManager(getContext(), numberOfColumns));
        adapter = new SeriesAdapter(seriesList, this::navigateToReadFragment);
        recyclerSeries.setAdapter(adapter);
    }

    private void loadViews() {
        displayUserGreeting();
        fetchAndDisplaySeries();
    }

    private void setupListeners() {
        // Listeners already set in setupViews (toolbar) and setupRecyclerView (adapter)
    }

    private void navigateToReadFragment(Series series) {
        if (!isViewCreated || getContext() == null)
            return;

        Bundle args = createReadFragmentArgs(series);
        ReadFragment readFragment = ReadFragment.newInstance(args);

        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, readFragment)
                .addToBackStack(null)
                .commit();
    }

    private Bundle createReadFragmentArgs(Series series) {
        Bundle args = new Bundle();
        args.putString("STORY_ID", storyId);
        args.putString("STORY_TITLE", storyName);
        args.putString("STORY_AUTHOR", storyAuthor);
        args.putString("STORY_CATEGORY", storyCategory);
        args.putString("STORY_DESCRIPTION", storyDescription);
        args.putString("STORY_IMAGE_URL", storyImageUrl);
        args.putString("PDF_LINK", series.getLink());
        args.putString("TAP", series.getName());
        return args;
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
                if (!isViewCreated || getContext() == null)
                    return;
                String userName = findUserNameByEmail(snapshot, userEmail);
                displayGreetingWithName(userName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isViewCreated || getContext() == null)
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
            } catch (Exception e) {
                Log.e("SeriesFragment", "Error decrypting: " + e.getMessage());
            }
        }
        return getString(R.string.default_user_name);
    }

    private String extractUserName(DataSnapshot userSnap) {
        String encryptedName = userSnap.child("fullName").getValue(String.class);
        if (encryptedName != null && !encryptedName.isEmpty()) {
            try {
                return Encryption.decrypt(encryptedName.trim());
            } catch (Exception e) {
                Log.e("SeriesFragment", "Error decrypting name: " + e.getMessage());
            }
        }
        return getString(R.string.default_user_name);
    }

    @SuppressLint("SetTextI18n")
    private void displayGreetingWithName(String userName) {
        String greeting = getGreetingByTime();
        textGreeting.setText(greeting + ", " + userName + "!");
    }

    private String getGreetingByTime() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11)
            return getString(R.string.greeting_morning);
        if (hour < 13)
            return getString(R.string.greeting_noon);
        if (hour < 18)
            return getString(R.string.greeting_afternoon);
        return getString(R.string.greeting_evening);
    }

    private void fetchAndDisplaySeries() {
        if (storyId == null || firestore == null)
            return;

        firestore.collection("story")
                .document(storyId)
                .collection("Series")
                .orderBy("name")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isViewCreated || getContext() == null)
                        return;
                    updateSeriesList(queryDocumentSnapshots);
                    displaySeriesList();
                })
                .addOnFailureListener(e -> {
                    if (!isViewCreated || getContext() == null)
                        return;
                    handleSeriesLoadError();
                });
    }

    private void updateSeriesList(Iterable<QueryDocumentSnapshot> documents) {
        seriesList.clear();
        for (QueryDocumentSnapshot doc : documents) {
            Series series = doc.toObject(Series.class);
            series.setId(doc.getId());
            if (series.getName() != null && series.getLink() != null) {
                seriesList.add(series);
            }
        }
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    private void displaySeriesList() {
        if (seriesList.isEmpty()) {
            showEmptyState(getString(R.string.no_series));
        } else {
            textNoSeries.setVisibility(View.GONE);
            recyclerSeries.setVisibility(View.VISIBLE);
        }
    }

    private void handleSeriesLoadError() {
        if (seriesList.isEmpty()) {
            showEmptyState(getString(R.string.error_loading_data));
        } else {
            Toast.makeText(getContext(), getString(R.string.error_loading_data), Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyState(String message) {
        textNoSeries.setText(message);
        textNoSeries.setVisibility(View.VISIBLE);
        recyclerSeries.setVisibility(View.GONE);
    }
}
