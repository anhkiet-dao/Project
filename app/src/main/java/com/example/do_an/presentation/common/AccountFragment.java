package com.example.do_an.presentation.common;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.do_an.R;
import com.example.do_an.core.constant.FirebaseConstants;
import com.example.do_an.core.utils.Encryption;
import com.example.do_an.presentation.auth.login.LoginActivity;
import com.example.do_an.presentation.chatbot.ChatFragment;
import com.example.do_an.presentation.profile.ProfileFragment;
import com.example.do_an.presentation.profile.settings.SettingFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AccountFragment extends Fragment {

    private TextView textProfile, textSettings, textInformation, textUsername, textChat;
    private Button btnLogOut;
    private ImageView imageAvatar;
    private View fragmentContainer;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FragmentManager.OnBackStackChangedListener backStackListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        return inflater.inflate(R.layout.ui_account_delay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        if (checkAuthentication()) {
            loadViews();
            setupListeners();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshViews();
        if (backStackListener == null) {
            backStackListener = () -> {
                int backStackEntryCount = requireActivity().getSupportFragmentManager().getBackStackEntryCount();
                if (backStackEntryCount == 0) {
                    if (fragmentContainer.getVisibility() == View.VISIBLE) {
                        fragmentContainer.setVisibility(View.GONE);
                    }
                } else {
                    Fragment frag = requireActivity().getSupportFragmentManager()
                            .findFragmentById(R.id.fragmentContainer);
                    if (frag == null && fragmentContainer.getVisibility() == View.VISIBLE) {
                        fragmentContainer.setVisibility(View.GONE);
                    }
                }
            };
        }
        requireActivity().getSupportFragmentManager().addOnBackStackChangedListener(backStackListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().getSupportFragmentManager().removeOnBackStackChangedListener(backStackListener);
    }

    private void setupViews(View view) {
        imageAvatar = view.findViewById(R.id.imageAvatar);
        textProfile = view.findViewById(R.id.textProfile);
        textSettings = view.findViewById(R.id.textSettings);
        textInformation = view.findViewById(R.id.textInformation);
        btnLogOut = view.findViewById(R.id.btnLogOut);
        textUsername = view.findViewById(R.id.textUsername);
        fragmentContainer = view.findViewById(R.id.fragmentContainer);
        textChat = view.findViewById(R.id.textChat);

        fragmentContainer.setVisibility(View.GONE);
    }

    private boolean checkAuthentication() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return false;
        }
        userRef = FirebaseDatabase
                .getInstance(FirebaseConstants.DATABASE_URL)
                .getReference(FirebaseConstants.USERS_PATH)
                .child(currentUser.getUid());
        return true;
    }

    private void loadViews() {
        fetchAndDisplayAvatar();
        fetchAndDisplayUsername();
        refreshViews();
    }

    private void navigateToLogin() {
        startActivity(new Intent(getActivity(), LoginActivity.class));
        if (getActivity() != null)
            getActivity().finish();
    }

    private void setupListeners() {
        textProfile.setOnClickListener(v -> openChildFragment(new ProfileFragment()));
        textInformation.setOnClickListener(v -> openChildFragment(new InforAppFragment()));
        textSettings.setOnClickListener(v -> openChildFragment(new SettingFragment()));
        textChat.setOnClickListener(v -> openChildFragment(new ChatFragment()));

        btnLogOut.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null)
                getActivity().finish();
        });
    }

    public void refreshViews() {
        if (textProfile != null)
            textProfile.setText(getString(R.string.profile));
        if (textSettings != null)
            textSettings.setText(getString(R.string.settings));
        if (textInformation != null)
            textInformation.setText(getString(R.string.information));
        if (btnLogOut != null)
            btnLogOut.setText(getString(R.string.logout));
        fetchAndDisplayUsername();
    }

    public void resetToMainScreen() {
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentContainer.setVisibility(View.GONE);
        }
    }

    private void openChildFragment(Fragment fragment) {
        fragmentContainer.setVisibility(View.VISIBLE);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void fetchAndDisplayUsername() {
        String USER_STRING = getString(R.string.user);
        userRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String encryptedName = snapshot.getValue(String.class);
                String displayName = (encryptedName != null && !encryptedName.isEmpty())
                        ? Encryption.decrypt(encryptedName)
                        : USER_STRING;
                textUsername.setText(displayName);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textUsername.setText(USER_STRING);
            }
        });
    }

    private void fetchAndDisplayAvatar() {
        userRef.child("avatarBase64").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String base64Data = snapshot.getValue(String.class);
                displayAvatar(base64Data);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setDefaultAvatar();
            }
        });
    }

    private void displayAvatar(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            setDefaultAvatar();
            return;
        }
        try {
            byte[] decodedBytes = Base64.decode(base64Data, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                imageAvatar.setImageBitmap(getCircularBitmap(bitmap));
            } else {
                setDefaultAvatar();
            }
        } catch (Exception e) {
            setDefaultAvatar();
        }
    }

    private void setDefaultAvatar() {
        imageAvatar.setImageResource(R.drawable.ic_logo_uit);
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);
        return output;
    }
}
