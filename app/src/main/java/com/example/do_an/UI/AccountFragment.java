package com.example.do_an.UI;

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
import com.example.do_an.application.Encryption;
import com.example.do_an.application.InforAppFragment;
import com.example.do_an.auth.LoginActivity;
import com.example.do_an.chatbot.ChatFragment;
import com.example.do_an.setting.SettingFragment;
import com.example.do_an.user.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class AccountFragment extends Fragment {

    private TextView tvProfile, tvSettings, tvInformation, tvUsername, tvChat;
    private Button btnLogout;
    private ImageView imgAvatar;
    private View fragmentContainer;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ui_account_delay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvProfile = view.findViewById(R.id.tvProfile);
        tvSettings = view.findViewById(R.id.tvSettings);
        tvInformation = view.findViewById(R.id.tvInformation);
        btnLogout = view.findViewById(R.id.btnLogout);
        tvUsername = view.findViewById(R.id.tvUsername);
        fragmentContainer = view.findViewById(R.id.fragment_container);
        tvChat = view.findViewById(R.id.tvChat);

        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        userRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(currentUser.getUid());

        loadAvatar();
        loadUsername();

        tvProfile.setOnClickListener(v -> openChildFragment(new ProfileFragment()));
        tvInformation.setOnClickListener(v -> openChildFragment(new InforAppFragment()));
        tvSettings.setOnClickListener(v -> openChildFragment(new SettingFragment()));
        tvChat.setOnClickListener(v -> openChildFragment(new ChatFragment()));

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTexts();
    }

    public void updateTexts() {
        tvProfile.setText(getString(R.string.profile));
        tvSettings.setText(getString(R.string.settings));
        tvInformation.setText(getString(R.string.informaton));
        btnLogout.setText(getString(R.string.logout));
        loadUsername();
    }

    public void resetToMainScreen() {
        if (fragmentContainer == null) return;
        FragmentManager fm = requireActivity().getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fragmentContainer.setVisibility(View.GONE);
        }
    }

    private void openChildFragment(Fragment fragment) {
        if (fragmentContainer == null) return;
        fragmentContainer.setVisibility(View.VISIBLE);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadUsername() {
        if (userRef == null) return;
        userRef.child("fullName").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.getValue(String.class);
                if (username != null && !username.isEmpty()) {
                    tvUsername.setText(Encryption.decrypt(username));
                } else {
                    tvUsername.setText("Người dùng");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUsername.setText("Người dùng");
            }
        });
    }

    private void loadAvatar() {
        if (userRef == null) return;
        userRef.child("avatarBase64").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String avatarBase64 = snapshot.getValue(String.class);
                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bitmap != null) imgAvatar.setImageBitmap(getCircularBitmap(bitmap));
                        else imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                    } catch (Exception e) {
                        imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                    }
                } else {
                    imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                imgAvatar.setImageResource(R.drawable.ic_logo_uit);
            }
        });
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
