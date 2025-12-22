package com.example.do_an.presentation.reading.note;

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
import com.example.do_an.domain.reading.model.NoteModel;
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
    private static final String TAG = "NoteFragment";

    private String uniqueNoteKey;
    private int pageNumber;
    private String storyTitleDisplay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.note_popup_note, container, false);

        edtNote = view.findViewById(R.id.edtNoteContent);
        btnAdd = view.findViewById(R.id.btnAddNote);
        btnUpdate = view.findViewById(R.id.btnUpdateNote);
        btnDelete = view.findViewById(R.id.btnDeleteNote);
        btnClose = view.findViewById(R.id.btnClose);
        txtTitleNote = view.findViewById(R.id.txtTitle);

        // Thiết lập text đa ngôn ngữ
        txtTitleNote.setText(getString(R.string.txtTitle));
        edtNote.setHint(getString(R.string.edtNoteContent));
        btnAdd.setText(getString(R.string.btnAddNote));
        btnUpdate.setText(getString(R.string.btnUpdateNote));
        btnDelete.setText(getString(R.string.btnDeleteNote));
        btnClose.setContentDescription(getString(R.string.btnClose));

        View dim = view.findViewById(R.id.dimBackground);
        dim.setOnClickListener(v -> closeFragment());

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
                Toast.makeText(getContext(), getString(R.string.error_missing_data), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Missing required data");
                return view;
            } else {
                uniqueNoteKey = noteContextId + "_PAGE_" + pageNumber;

                txtTitleNote.setText(storyTitleDisplay + " - " + getString(R.string.page_label) + " " + pageNumber);

                String firebaseUserKey = userEmail.replace('.', '_').replace('@', '_');
                notesRef = FirebaseDatabase.getInstance().getReference("users")
                        .child(firebaseUserKey)
                        .child("notes")
                        .child(uniqueNoteKey);

                loadNoteFromFirebase();

                btnAdd.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
                btnUpdate.setOnClickListener(v -> saveNoteToFirebase(edtNote.getText().toString().trim()));
                btnDelete.setOnClickListener(v -> deleteNoteFromFirebase());
                btnClose.setOnClickListener(v -> closeFragment());
            }
        } else {
            Toast.makeText(getContext(), getString(R.string.error_no_fragment_data), Toast.LENGTH_LONG).show();
        }

        return view;
    }

    private void closeFragment() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }

    private void loadNoteFromFirebase() {
        notesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    NoteModel note = dataSnapshot.getValue(NoteModel.class);
                    if (note != null && note.content != null) {
                        edtNote.setText(note.content);
                        updateButtonStates(false);
                        Log.d(TAG, "Note loaded");
                    }
                } else {
                    updateButtonStates(true);
                    Log.d(TAG, "No note found");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), getString(R.string.error_load_note, databaseError.getMessage()), Toast.LENGTH_LONG).show();
                updateButtonStates(true);
            }
        });
    }

    private void saveNoteToFirebase(String content) {
        if (content.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.error_empty_note), Toast.LENGTH_SHORT).show();
            return;
        }

        String baseNoteContextId = uniqueNoteKey.substring(0, uniqueNoteKey.lastIndexOf("_PAGE_"));
        NoteModel note = new NoteModel(userEmail, baseNoteContextId, pageNumber, content, System.currentTimeMillis());

        notesRef.setValue(note)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), getString(R.string.note_saved, pageNumber), Toast.LENGTH_SHORT).show();
                    updateButtonStates(false);
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_save_note, e.getMessage()), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save note", e);
                });
    }

    private void deleteNoteFromFirebase() {
        notesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    edtNote.setText("");
                    Toast.makeText(getContext(), getString(R.string.note_deleted, pageNumber), Toast.LENGTH_SHORT).show();
                    updateButtonStates(true);
                    hideKeyboardAndClearFocus();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_delete_note), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete note", e);
                });
    }

    private void updateButtonStates(boolean noNote) {
        btnAdd.setVisibility(noNote ? View.VISIBLE : View.GONE);
        btnUpdate.setVisibility(noNote ? View.GONE : View.VISIBLE);
        btnDelete.setVisibility(noNote ? View.GONE : View.VISIBLE);
    }

    private void hideKeyboardAndClearFocus() {
        edtNote.clearFocus();
        View view = getView();
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
