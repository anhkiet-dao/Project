package com.example.do_an.application;

import android.content.Context; // <<< THÊM: Dùng cho InputMethodManager
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager; // <<< THÊM: Dùng để ẩn bàn phím
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.application.NoteModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NoteActivity extends AppCompatActivity {

    private EditText edtNote;
    private ImageView btnClose;
    private TextView btnAdd, btnUpdate, btnDelete, txtTitleNote;

    private String userEmail;
    private DatabaseReference notesRef;
    private static final String TAG = "NoteActivity";

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
        txtTitleNote = findViewById(R.id.txtTitle);

        String noteContextId = getIntent().getStringExtra("NOTE_CONTEXT_ID");
        pageNumber = getIntent().getIntExtra("PAGE_NUMBER", 0);
        storyTitleDisplay = getIntent().getStringExtra("STORY_TITLE_DISPLAY");

        // Lấy Email người dùng đã đăng nhập
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmail = currentUser.getEmail();
        }

        if (noteContextId == null || pageNumber <= 0 || userEmail == null) {
            Toast.makeText(this, "Lỗi: Cần thông tin đăng nhập hoặc trang ghi chú.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        uniqueNoteKey = noteContextId + "_PAGE_" + pageNumber;

        if (txtTitleNote != null) {
            txtTitleNote.setText(storyTitleDisplay + " - Trang " + pageNumber);
            txtTitleNote.setVisibility(View.VISIBLE);
        }

        // --- Bắt đầu Logic Firebase ---
        String firebaseUserKey = userEmail.replace('.', '_').replace('@', '_');

        notesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(firebaseUserKey)
                .child("notes")
                .child(uniqueNoteKey);

        loadNoteFromFirebase();
        // --- Kết thúc Logic Firebase ---

        // Cập nhật Listener để gọi hàm Firebase
        btnAdd.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
        btnUpdate.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
        btnDelete.setOnClickListener(v -> deleteNoteFromFirebase());

        btnClose.setOnClickListener(v -> finish());
    }

    // --- Phương thức Firebase: Tải ghi chú ---
    private void loadNoteFromFirebase() {
        notesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    NoteModel note = dataSnapshot.getValue(NoteModel.class);
                    if (note != null && note.content != null) {
                        edtNote.setText(note.content);
                        updateButtonStates(false); // Có ghi chú
                        Log.d(TAG, "Ghi chú đã tải.");
                    }
                } else {
                    updateButtonStates(true); // Chưa có ghi chú
                    Log.d(TAG, "Không tìm thấy ghi chú.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(NoteActivity.this, "Lỗi tải ghi chú: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateButtonStates(true);
            }
        });
    }

    // --- Phương thức Firebase: Lưu/Cập nhật ghi chú ---
    private void saveNoteToFirebase(String content) {
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tách noteContextId (ID truyện + Tên tập)
        String baseNoteContextId = uniqueNoteKey.substring(0, uniqueNoteKey.lastIndexOf("_PAGE_"));

        NoteModel note = new NoteModel(
                userEmail,
                baseNoteContextId,
                pageNumber,
                content,
                System.currentTimeMillis()
        );

        notesRef.setValue(note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(NoteActivity.this, "Đã lưu ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();
                    updateButtonStates(false);
                    // ✅ THÊM: Thu hồi focus và ẩn bàn phím sau khi lưu thành công
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NoteActivity.this, "Lưu ghi chú thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Lỗi lưu Firebase", e);
                });
    }

    // --- Phương thức Firebase: Xóa ghi chú ---
    private void deleteNoteFromFirebase() {
        notesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    edtNote.setText("");
                    Toast.makeText(NoteActivity.this, "Đã xóa ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();
                    updateButtonStates(true);
                    // ✅ THÊM: Thu hồi focus và ẩn bàn phím sau khi xóa thành công
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NoteActivity.this, "Xóa ghi chú thất bại.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Lỗi xóa Firebase", e);
                });
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

    // --- PHƯƠNG THỨC MỚI: Ẩn bàn phím và bỏ focus ---
    private void hideKeyboardAndClearFocus() {
        // 1. Bỏ focus khỏi EditText (để dấu nháy biến mất)
        edtNote.clearFocus();

        // 2. Ẩn bàn phím ảo
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}