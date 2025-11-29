package com.example.do_an.UI;

import android.os.Bundle;
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

public class MyListFragment extends Fragment {

    private Button btnReadlist, btnHistory, btnFavorite, btnDownload;
    EditText edtSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_mylist, container, false);

        btnReadlist = view.findViewById(R.id.btnReadlist);
        btnHistory = view.findViewById(R.id.btnHistory);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnDownload = view.findViewById(R.id.btnDownload);
        edtSearch = view.findViewById(R.id.edtSearch);

        btnReadlist.setOnClickListener(v -> {
            loadFragment(new ReadlistFragment());
            selectButton(btnReadlist);
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

        // Fragment mặc định
        loadFragment(new ReadlistFragment());
        selectButton(btnReadlist);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Fragment child = getChildFragmentManager().findFragmentById(R.id.containerContent);
                if (child instanceof ReadlistFragment) {
                    ((ReadlistFragment) child).onSearch(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void selectButton(Button selected) {
        btnReadlist.setBackgroundResource(R.drawable.button_unselected);
        btnHistory.setBackgroundResource(R.drawable.button_unselected);
        btnFavorite.setBackgroundResource(R.drawable.button_unselected);
        btnDownload.setBackgroundResource(R.drawable.button_unselected);

        btnReadlist.setTextColor(getResources().getColor(android.R.color.white));
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
}
