package com.example.do_an.user;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.do_an.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.example.do_an.application.Encryption;

import java.io.ByteArrayOutputStream;

public class ProfileFragment extends Fragment { // Đã chuyển thành Fragment

    private TextView tvFullName, tvGender, tvBirthDate, tvPhone, tvEmail, tvInterest;
    private ImageView imgAvatar;
    private Button btnLogout;

    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    // 1. Inflate layout trong onCreateView
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    // 2. Ánh xạ view và thực hiện logic trong onViewCreated
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ view (sử dụng view đã inflate)
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvFullName = view.findViewById(R.id.tvFullName);
        tvGender = view.findViewById(R.id.tvGender);
        tvBirthDate = view.findViewById(R.id.tvBirthDate);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvInterest = view.findViewById(R.id.tvInterest);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Firebase Auth
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Sử dụng getContext() và getActivity()
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }

        String userId = currentUser.getUid();

        // Firebase Database reference
        databaseRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userId);

        // Hiển thị email
        tvEmail.setText(currentUser.getEmail());

        // Lấy dữ liệu người dùng
        loadUserInfo();

        // 🔹 Khi nhấn vào ảnh — cho phép chọn ảnh mới
        imgAvatar.setOnClickListener(v -> {
            // Sử dụng getContext()
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Thay đổi ảnh đại diện");
            builder.setMessage("Bạn có muốn chọn ảnh mới không?");
            builder.setPositiveButton("Chọn ảnh", (dialog, which) -> openImagePicker());
            builder.setNegativeButton("Hủy", null);
            builder.show();
        });

        // Xử lý đăng xuất
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            // Sử dụng getContext() và getActivity()
            Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void loadUserInfo() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Sử dụng getContext()
                    Toast.makeText(getContext(), "Không tìm thấy dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
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
                            // getResources() là của Fragment, không cần ProfileActivity.this
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
                    // Sử dụng getContext()
                    Toast.makeText(getContext(), "Lỗi khi tải dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                    imgAvatar.setImageResource(R.drawable.avatar);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Sử dụng getContext()
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Mở trình chọn ảnh
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        // Gọi startActivityForResult từ Fragment
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // 🔹 Nhận ảnh đã chọn (Fragment nhận kết quả trực tiếp)
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Thay đổi RESULT_OK từ Activity sang hằng số Activity.RESULT_OK
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                // Sử dụng requireActivity().getContentResolver()
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
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
                // Sử dụng getContext()
                Toast.makeText(getContext(), "Lỗi khi chọn ảnh!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🔹 Lưu Base64 lên Firebase
    private void uploadImageToFirebase(String encodedImage) {
        if (currentUser == null) return;

        databaseRef.child("avatarBase64").setValue(encodedImage)
                // Sử dụng getContext()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi khi lưu ảnh!", Toast.LENGTH_SHORT).show());
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