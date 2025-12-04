package com.example.do_an.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.download.DownloadFragment;
import com.example.do_an.favorite.FavoriteFragment;
import com.example.do_an.history.HistoryFragment;
import com.example.do_an.story.ReadlistFragment;

import java.util.ArrayList;
import java.util.List;

public class MyListFragment extends Fragment {

    private Button btnReadList;
    private Button btnHistory;
    private Button btnFavorite;
    private Button btnDownload;

    EditText edtSearch;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<Button> tabButtons = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.ui_activity_mylist, container, false);

        btnReadList = view.findViewById(R.id.btn_readlist);
        btnHistory = view.findViewById(R.id.btn_history);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        btnDownload = view.findViewById(R.id.btn_download);
        edtSearch = view.findViewById(R.id.edt_search);

        initTabs();

        loadFragment(new ReadlistFragment());
        selectButton(btnReadList);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    Fragment child = getChildFragmentManager().findFragmentById(R.id.frame_main);
                    if (child instanceof ReadlistFragment) {
                        ((ReadlistFragment) child).onFilterKeyword(s.toString());
                    }
                };

                handler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void initTabs() {
        btnReadList.setOnClickListener(v -> {
            loadFragment(new ReadlistFragment());
            selectButton(btnReadList);
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

        tabButtons.add(btnReadList);
        tabButtons.add(btnHistory);
        tabButtons.add(btnFavorite);
        tabButtons.add(btnDownload);
    }

    private void selectButton(Button selected) {
        for (Button btn : tabButtons) {
            btn.setBackgroundResource(R.drawable.button_unselected);
            btn.setTextColor(getResources().getColor(android.R.color.white));
        }

        selected.setBackgroundResource(R.drawable.button_selected);
        selected.setTextColor(getResources().getColor(android.R.color.holo_blue_bright));
    }

    private void loadFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_main, fragment)
                .commit();
    }
}
