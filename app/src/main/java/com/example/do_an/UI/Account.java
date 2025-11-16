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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.do_an.R;
import com.example.do_an.Story.FavoriteActivity;
import com.example.do_an.application.HistoryActivity;
import com.example.do_an.application.InforApp;
import com.example.do_an.user.LoginActivity;
import com.example.do_an.user.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Account extends AppCompatActivity {

    TextView tvProfile, tvSettings, tvHistory, tvAnalytics, tvInformation, tvFavorite;
    Button btnLogout;
    ImageView imgAvatar;

    FirebaseAuth auth;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_delay);

        // Ánh xạ view
        imgAvatar = findViewById(R.id.imgAvatar);
        tvProfile = findViewById(R.id.tvProfile);
        tvSettings = findViewById(R.id.tvSettings);
        tvHistory = findViewById(R.id.tvHistory);
        tvAnalytics = findViewById(R.id.tvAnalytics);
        tvInformation = findViewById(R.id.tvInformation);
        btnLogout = findViewById(R.id.btnLogout);
        tvFavorite = findViewById(R.id.tvLike);

        // Firebase Auth
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Account.this, LoginActivity.class));
            finish();
            return;
        }

        String uid = currentUser.getUid();

        // 🔹 Kết nối đến Realtime Database
        userRef = FirebaseDatabase
                .getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(uid);

        // 🔹 Lấy ảnh avatarBase64
        userRef.child("avatarBase64").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String avatarBase64 = snapshot.getValue(String.class);

                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                        Bitmap originalBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        if (originalBitmap != null) {
                            Bitmap circleBitmap = getCircularBitmap(originalBitmap);
                            imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgAvatar.setImageBitmap(circleBitmap);
                        } else {
                            imgAvatar.setImageResource(R.drawable.avatar);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        imgAvatar.setImageResource(R.drawable.avatar);
                    }
                } else {
                    imgAvatar.setImageResource(R.drawable.avatar);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                imgAvatar.setImageResource(R.drawable.avatar);
                Toast.makeText(Account.this, "Không thể tải ảnh: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Khi bấm vào "Xem hồ sơ"
        tvProfile.setOnClickListener(v -> startActivity(new Intent(Account.this, ProfileActivity.class)));

        // Khi bấm vào "Thông tin ứng dụng"
        tvInformation.setOnClickListener(v -> startActivity(new Intent(Account.this, InforApp.class)));

        // Khi bấm vào nút "Đăng xuất"
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(Account.this, LoginActivity.class));
            finish();
        });

        //khi bam yeu thich
        tvFavorite.setOnClickListener(v ->
                startActivity(new Intent(Account.this, FavoriteActivity.class))
        );

        tvHistory.setOnClickListener(v -> startActivity((new Intent(Account.this, HistoryActivity.class))));
    }

    // 🔹 Hàm cắt bitmap thành hình tròn
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }
}
