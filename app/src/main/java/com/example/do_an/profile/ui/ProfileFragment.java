package com.example.do_an.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.do_an.R;
import com.example.do_an.core.encryption.Encryption;
import com.example.do_an.auth.LoginActivity;
import com.example.do_an.profile.utils.ProfileAvatarUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment { // Đã chuyển thành Fragment

    private TextView tvFullname, tvGender, tvBirthDate,
            tvPhone, tvEmail, tvInterest;

    private ImageView imgAvatar;
    private Button btnLogout;

    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgAvatar = view.findViewById(R.id.img_avatar);
        tvFullname = view.findViewById(R.id.tv_fullname);
        tvGender = view.findViewById(R.id.tv_gender);
        tvBirthDate = view.findViewById(R.id.tv_birthdate);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvEmail = view.findViewById(R.id.tv_email);
        tvInterest = view.findViewById(R.id.tv_interest);
        btnLogout = view.findViewById(R.id.btn_logout);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }

        databaseRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(currentUser.getUid());

        tvEmail.setText(currentUser.getEmail());

        loadUserInfo();

        imgAvatar.setOnClickListener(v -> handleChangeAvatar());
        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(getContext(), "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
        if (getActivity() != null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        }
    }

    private void handleChangeAvatar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Thay đổi ảnh đại diện");
        builder.setMessage("Bạn có muốn chọn ảnh mới không?");
        builder.setPositiveButton("Chọn ảnh", (dialog, which) -> openImagePicker());
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void loadUserInfo() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
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

                    tvFullname.setText(fullName != null ? Encryption.decrypt(fullName) : "Chưa cập nhật");
                    tvGender.setText(gender != null ? Encryption.decrypt(gender) : "Chưa cập nhật");
                    tvBirthDate.setText(birthDate != null ? Encryption.decrypt(birthDate) : "Chưa cập nhật");
                    tvPhone.setText(phone != null ? Encryption.decrypt(phone) : "Chưa cập nhật");
                    tvInterest.setText(interest != null ? Encryption.decrypt(interest) : "Chưa cập nhật");

                    displayAvatar(avatarBase64);

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi khi tải dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                    imgAvatar.setImageResource(R.drawable.avatar);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayAvatar(String avatarBase64) {
        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
            byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            if (originalBitmap != null) {
                int sizeInPx = (int) (120 * getResources().getDisplayMetrics().density);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, sizeInPx, sizeInPx, true);
                Bitmap circleBitmap = ProfileAvatarUtil.getCircularBitmap(scaledBitmap);

                imgAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgAvatar.setImageBitmap(circleBitmap);
            } else {
                imgAvatar.setImageResource(R.drawable.avatar);
            }
        } else {
            imgAvatar.setImageResource(R.drawable.avatar);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST) {
            getActivity();
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri imageUri = data.getData();

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
                    Bitmap circleBitmap = ProfileAvatarUtil.getCircularBitmap(resizedBitmap);

                    imgAvatar.setImageBitmap(circleBitmap);

                    String encodedImage = ProfileAvatarUtil.encodeBase64(resizedBitmap);
                    uploadImageToFirebase(encodedImage);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi khi chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void uploadImageToFirebase(String encodedImage) {
        if (currentUser == null) return;
        databaseRef.child("avatarBase64")
                .setValue(encodedImage)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(), "Cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi khi lưu ảnh!", Toast.LENGTH_SHORT).show());
    }
}