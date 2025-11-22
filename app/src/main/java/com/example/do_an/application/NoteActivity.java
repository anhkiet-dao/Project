package com.example.do_an.application;

import android.os.Bundle;
import android.util.Log; // <<< THÊM: Dùng để debug
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.application.NoteModel; // <<< THÊM: Import Model
import com.google.firebase.auth.FirebaseAuth; // <<< THÊM: Dùng để lấy email
import com.google.firebase.auth.FirebaseUser; // <<< THÊM
import com.google.firebase.database.DataSnapshot; // <<< THÊM: Dùng cho Firebase Listener
import com.google.firebase.database.DatabaseError; // <<< THÊM
import com.google.firebase.database.DatabaseReference; // <<< THÊM
import com.google.firebase.database.FirebaseDatabase; // <<< THÊM
import com.google.firebase.database.ValueEventListener; // <<< THÊM

// Imports bị loại bỏ:
// import android.content.SharedPreferences;

public class NoteActivity extends AppCompatActivity {

    private EditText edtNote;
    private ImageView btnClose;
    private TextView btnAdd, btnUpdate, btnDelete, txtTitleNote;

    // Các biến SharedPreferences bị loại bỏ:
    // private SharedPreferences pref;
    // private static final String PREF_NAME = "MY_NOTE_PER_PAGE";

    private String userEmail; // <<< THÊM: Lưu email người dùng
    private DatabaseReference notesRef; // <<< THÊM: Tham chiếu Firebase
    private static final String TAG = "NoteActivity"; // <<< THÊM

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
        txtTitleNote = findViewById(R.id.txtTitle); // Giả định ID này có trong popup_note

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
        // 1. Chuẩn bị Key Firebase an toàn
        // Thay thế ký tự không hợp lệ trong email để tạo khóa an toàn
        String firebaseUserKey = userEmail.replace('.', '_').replace('@', '_');

        // 2. Thiết lập tham chiếu Firebase: users/{email_key}/notes/{uniqueNoteKey}
        notesRef = FirebaseDatabase.getInstance().getReference("users")
                .child(firebaseUserKey)
                .child("notes")
                .child(uniqueNoteKey);

        // 3. Tải ghi chú hiện có từ Firebase
        loadNoteFromFirebase();
        // --- Kết thúc Logic Firebase ---

        // Logic SharedPreferences bị loại bỏ:
        // pref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        // String savedNote = pref.getString(uniqueNoteKey, "");
        // edtNote.setText(savedNote);
        // updateButtonStates(savedNote.isEmpty());

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
}