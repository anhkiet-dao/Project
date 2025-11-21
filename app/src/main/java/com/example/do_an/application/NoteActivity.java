package com.example.do_an.application;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;

public class NoteActivity extends AppCompatActivity {

    private EditText edtNote;
    private ImageView btnClose;
    private TextView btnAdd, btnUpdate, btnDelete, txtTitleNote;

    private SharedPreferences pref;
    private static final String PREF_NAME = "MY_NOTE_PER_PAGE";

    private String uniqueNoteKey;
    private int pageNumber;
    private String storyTitleDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_note);

        edtNote = findViewById(R.id.edtNoteContent);
        btnAdd = findViewById(R.id.btnAddNote);
        btnUpdate = findViewById(R.id.btnUpdateNote);
        btnDelete = findViewById(R.id.btnDeleteNote);
        btnClose = findViewById(R.id.btnClose);

        String noteContextId = getIntent().getStringExtra("NOTE_CONTEXT_ID");
        pageNumber = getIntent().getIntExtra("PAGE_NUMBER", 0);
        storyTitleDisplay = getIntent().getStringExtra("STORY_TITLE_DISPLAY");

        if (noteContextId == null || pageNumber <= 0) {
            Toast.makeText(this, "Lỗi: Không xác định được trang ghi chú.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        uniqueNoteKey = noteContextId + "_PAGE_" + pageNumber;

        if (txtTitleNote != null) {
            txtTitleNote.setText(storyTitleDisplay + " - Trang " + pageNumber);
            txtTitleNote.setVisibility(View.VISIBLE);
        }

        pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        String savedNote = pref.getString(uniqueNoteKey, "");
        edtNote.setText(savedNote);

        updateButtonStates(savedNote.isEmpty());

        btnAdd.setOnClickListener(v -> {
            String text = edtNote.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
                return;
            }

            pref.edit().putString(uniqueNoteKey, text).apply();
            Toast.makeText(this, "Đã thêm ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();

            updateButtonStates(false);
        });

        btnUpdate.setOnClickListener(v -> {
            String text = edtNote.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
                return;
            }

            pref.edit().putString(uniqueNoteKey, text).apply();
            Toast.makeText(this, "Đã sửa ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();
        });

        btnDelete.setOnClickListener(v -> {
            pref.edit().remove(uniqueNoteKey).apply();
            edtNote.setText("");

            Toast.makeText(this, "Đã xóa ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();

            updateButtonStates(true);
        });

        btnClose.setOnClickListener(v -> finish());
    }

    private void updateButtonStates(boolean noNote) {
        if (noNote) {
            btnAdd.setVisibility(View.VISIBLE);
            btnUpdate.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        } else {
            btnAdd.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        }
    }
}
