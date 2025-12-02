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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.do_an.R;
import com.example.do_an.Statistic.StatisticFragment;
import com.example.do_an.application.Encryption;
import com.example.do_an.application.InforAppFragment;
import com.example.do_an.auth.LoginActivity;
import com.example.do_an.user.ProfileFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class AccountFragment extends Fragment {

    private TextView tvProfile, tvSettings, tvAnalytics, tvInformation, tvUsername;
    private Button btnLogout;
    private ImageView imgAvatar;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    private View fragmentContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_delay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvProfile = view.findViewById(R.id.tvProfile);
        tvSettings = view.findViewById(R.id.tvSettings);
        tvAnalytics = view.findViewById(R.id.tvAnalytics);
        tvInformation = view.findViewById(R.id.tvInformation);
        btnLogout = view.findViewById(R.id.btnLogout);
        tvUsername = view.findViewById(R.id.tvUsername);

        fragmentContainer = view.findViewById(R.id.fragment_container);

        // Thiết lập trạng thái ban đầu
        if (fragmentContainer != null) {
            // Đảm bảo container ẩn khi fragment mới được tạo
            fragmentContainer.setVisibility(View.GONE);
        }

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        String uid = currentUser.getUid();
        userRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(uid);

        loadAvatar();
        loadUsername();

        tvProfile.setOnClickListener(v -> openChildFragment(new ProfileFragment()));
        tvInformation.setOnClickListener(v -> openChildFragment(new InforAppFragment()));
        tvAnalytics.setOnClickListener(v -> openChildFragment(new StatisticFragment()));

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        requireActivity().getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (fragmentContainer != null) {
                // Chỉ ẩn container khi Back Stack của Activity rỗng (tức là quay về màn hình AccountFragment gốc)
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    fragmentContainer.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadUsername() {
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
                Toast.makeText(getContext(), "Không thể tải tên: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void resetToMainScreen() {
        if (fragmentContainer != null) {
            FragmentManager fm = requireActivity().getSupportFragmentManager();

            // Kiểm tra xem Back Stack có Fragment con nào không
            if (fm.getBackStackEntryCount() > 0) {
                // Xóa tất cả các Fragment con đang có trong Back Stack của Activity
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Sau khi pop, Back Stack Count sẽ là 0, và listener sẽ ẩn container, nhưng ta đảm bảo lại.
                if (fm.getBackStackEntryCount() == 0) {
                    fragmentContainer.setVisibility(View.GONE);
                }
            }
        }
    }

    // MỞ FRAGMENT CON
    private void openChildFragment(Fragment fragment) {
        if (fragmentContainer == null) return;

        fragmentContainer.setVisibility(View.VISIBLE);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Fragment con được thêm vào back stack của Activity
                .commit();
    }

    private void loadAvatar() {
        userRef.child("avatarBase64").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String avatarBase64 = snapshot.getValue(String.class);
                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bitmap != null) imgAvatar.setImageBitmap(getCircularBitmap(bitmap));
                        else imgAvatar.setImageResource(R.drawable.avatar);
                    } catch (Exception e) {
                        imgAvatar.setImageResource(R.drawable.avatar);
                    }
                } else {
                    imgAvatar.setImageResource(R.drawable.avatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                imgAvatar.setImageResource(R.drawable.avatar);
                Toast.makeText(getContext(), "Không thể tải ảnh: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Đã có lệnh return, giải quyết lỗi Cannot resolve symbol 'output'
        return output;
    }
}