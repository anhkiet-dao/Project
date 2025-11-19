package com.example.do_an.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import com.example.do_an.R;

public class MyList extends AppCompatActivity {

    private Button btnReadlist, btnHistory, btnFavorite, btnDownload;
    EditText edtSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mylist);

        btnReadlist = findViewById(R.id.btnReadlist);
        btnHistory = findViewById(R.id.btnHistory);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnDownload = findViewById(R.id.btnDownload);
        edtSearch = findViewById(R.id.edtSearch);

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

        loadFragment(new ReadlistFragment());
        selectButton(btnReadlist);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerContent);

                if (fragment instanceof ReadlistFragment) {
                    ((ReadlistFragment) fragment).onSearch(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

        // Giao diện cho nút đang được chọn
        selected.setBackgroundResource(R.drawable.button_selected);
        selected.setTextColor(getResources().getColor(android.R.color.holo_blue_bright));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerContent, fragment)
                .commit();
    }
}
