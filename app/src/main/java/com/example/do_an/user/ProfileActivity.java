package com.example.do_an.user;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.*;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.example.do_an.application.Encryption;

import java.io.ByteArrayOutputStream;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvGender, tvBirthDate, tvPhone, tvEmail, tvInterest;
    private ImageView imgAvatar;
    private Button btnLogout;

    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Ánh xạ view
        imgAvatar = findViewById(R.id.imgAvatar);
        tvFullName = findViewById(R.id.tvFullName);
        tvGender = findViewById(R.id.tvGender);
        tvBirthDate = findViewById(R.id.tvBirthDate);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvInterest = findViewById(R.id.tvInterest);
        btnLogout = findViewById(R.id.btnLogout);

        // Firebase Auth
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // Firebase Database reference
        databaseRef = FirebaseDatabase
                .getInstance("https://nt118-dd4f7-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userId);

        // Hiển thị email
        tvEmail.setText(currentUser.getEmail());

        // Lấy dữ liệu người dùng
        loadUserInfo();

        // 🔹 Khi nhấn vào ảnh — cho phép chọn ảnh mới
        imgAvatar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
            builder.setTitle("Thay đổi ảnh đại diện");
            builder.setMessage("Bạn có muốn chọn ảnh mới không?");
            builder.setPositiveButton("Chọn ảnh", (dialog, which) -> openImagePicker());
            builder.setNegativeButton("Hủy", null);
            builder.show();
        });

        // Xử lý đăng xuất
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(ProfileActivity.this, "Không tìm thấy dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String birthDate = snapshot.child("birthDate").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String interest = snapshot.child("interest").getValue(String.class);
                    String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);

                    // Hiển thị thông tin người dùng
                    tvFullName.setText(fullName != null ? Encryption.decrypt(fullName) : "Chưa cập nhật");
                    tvGender.setText(gender != null ? Encryption.decrypt(gender) : "Chưa cập nhật");
                    tvBirthDate.setText(birthDate != null ? Encryption.decrypt(birthDate) : "Chưa cập nhật");
                    tvPhone.setText(phone != null ? Encryption.decrypt(phone) : "Chưa cập nhật");
                    tvInterest.setText(interest != null ? Encryption.decrypt(interest) : "Chưa cập nhật");

                    // Giải mã và hiển thị ảnh đại diện hình tròn
                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                        Bitmap originalBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                        if (originalBitmap != null) {
                            int sizeInPx = (int) (120 * getResources().getDisplayMetrics().density);
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, sizeInPx, sizeInPx, true);
                            Bitmap circleBitmap = getCircularBitmap(scaledBitmap);

                            imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            imgAvatar.setImageBitmap(circleBitmap);
                        } else {
                            imgAvatar.setImageResource(R.drawable.avatar);
                        }
                    } else {
                        imgAvatar.setImageResource(R.drawable.avatar);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ProfileActivity.this, "Lỗi khi tải dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                    imgAvatar.setImageResource(R.drawable.avatar);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ProfileActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Mở trình chọn ảnh
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 🔹 Nhận ảnh đã chọn
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
                Bitmap circleBitmap = getCircularBitmap(resizedBitmap);

                imgAvatar.setImageBitmap(circleBitmap);

                // Encode sang Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                // Cập nhật lên Firebase
                uploadImageToFirebase(encodedImage);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi chọn ảnh!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🔹 Lưu Base64 lên Firebase
    private void uploadImageToFirebase(String encodedImage) {
        if (currentUser == null) return;

        databaseRef.child("avatarBase64").setValue(encodedImage)
                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi khi lưu ảnh!", Toast.LENGTH_SHORT).show());
    }

    // 🔹 Cắt ảnh tròn
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
