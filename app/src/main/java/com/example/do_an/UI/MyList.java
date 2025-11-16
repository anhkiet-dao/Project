package com.example.do_an.UI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.widget.Button;

import com.example.do_an.R;

public class MyList extends AppCompatActivity {

    private Button btnReadlist, btnHistory, btnFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mylist);

        btnReadlist = findViewById(R.id.btnReadlist);
        btnHistory = findViewById(R.id.btnHistory);
        btnFavorite = findViewById(R.id.btnFavorite);

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

        // Mặc định hiển thị Readlist và chọn nút Readlist
        loadFragment(new ReadlistFragment());
        selectButton(btnReadlist);
    }

    private void selectButton(Button selected) {
        btnReadlist.setSelected(false);
        btnHistory.setSelected(false);
        btnFavorite.setSelected(false);

        selected.setSelected(true);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.containerContent, fragment)
                .commit();
    }
}
