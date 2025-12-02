package com.example.do_an.UI;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.application.NoteModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NoteFragment extends Fragment {

    private EditText edtNote;
    private ImageView btnClose;
    private TextView btnAdd, btnUpdate, btnDelete, txtTitleNote;

    private String userEmail;
    private DatabaseReference notesRef;
    private static final String TAG = "NoteFragment"; // <<< THAY ĐỔI: Tên tag

    private String uniqueNoteKey;
    private int pageNumber;
    private String storyTitleDisplay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.popup_note, container, false);
        View dim = view.findViewById(R.id.dimBackground);
        dim.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        edtNote = view.findViewById(R.id.edtNoteContent);
        btnAdd = view.findViewById(R.id.btnAddNote);
        btnUpdate = view.findViewById(R.id.btnUpdateNote);
        btnDelete = view.findViewById(R.id.btnDeleteNote);
        btnClose = view.findViewById(R.id.btnClose);
        txtTitleNote = view.findViewById(R.id.txtTitle);

        Bundle args = getArguments();
        if (args != null) {
            String noteContextId = args.getString("NOTE_CONTEXT_ID");
            pageNumber = args.getInt("PAGE_NUMBER", 0);
            storyTitleDisplay = args.getString("STORY_TITLE_DISPLAY");

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                userEmail = currentUser.getEmail();
            }

            if (noteContextId == null || pageNumber <= 0 || userEmail == null) {
                Toast.makeText(getContext(), "Lỗi: Cần thông tin đăng nhập hoặc trang ghi chú.", Toast.LENGTH_LONG).show();
                if (getActivity() != null) {
                    Log.e(TAG, "Lỗi thiếu dữ liệu bắt buộc.");
                    return view;
                }
            } else {
                uniqueNoteKey = noteContextId + "_PAGE_" + pageNumber;

                if (txtTitleNote != null) {
                    txtTitleNote.setText(storyTitleDisplay + " - Trang " + pageNumber);
                    txtTitleNote.setVisibility(View.VISIBLE);
                }

                String firebaseUserKey = userEmail.replace('.', '_').replace('@', '_');

                notesRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(firebaseUserKey)
                        .child("notes")
                        .child(uniqueNoteKey);

                loadNoteFromFirebase();

                btnAdd.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
                btnUpdate.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
                btnDelete.setOnClickListener(v -> deleteNoteFromFirebase());

                btnClose.setOnClickListener(v -> {
                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                });
            }
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy dữ liệu Fragment.", Toast.LENGTH_LONG).show();
        }

        return view;
    }

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
                // Sử dụng getContext()
                Toast.makeText(getContext(), "Lỗi tải ghi chú: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateButtonStates(true);
            }
        });
    }

    private void saveNoteToFirebase(String content) {
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    Toast.makeText(getContext(), "Đã lưu ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();
                    updateButtonStates(false);
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lưu ghi chú thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Lỗi lưu Firebase", e);
                });
    }

    private void deleteNoteFromFirebase() {
        notesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    edtNote.setText("");
                    Toast.makeText(getContext(), "Đã xóa ghi chú ở trang " + pageNumber + "!", Toast.LENGTH_SHORT).show();
                    updateButtonStates(true);
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Xóa ghi chú thất bại.", Toast.LENGTH_SHORT).show();
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

    private void hideKeyboardAndClearFocus() {
        edtNote.clearFocus();

        View view = getView(); // Lấy View của Fragment
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static NoteFragment newInstance(String noteContextId, int pageNumber, String storyTitleDisplay) {
        NoteFragment fragment = new NoteFragment();
        Bundle args = new Bundle();
        args.putString("NOTE_CONTEXT_ID", noteContextId);
        args.putInt("PAGE_NUMBER", pageNumber);
        args.putString("STORY_TITLE_DISPLAY", storyTitleDisplay);
        fragment.setArguments(args);
        return fragment;
    }
}