package com.example.do_an.presentation.profile;

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
import com.example.do_an.presentation.auth.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.example.do_an.core.utils.Encryption;

import java.io.ByteArrayOutputStream;

public class ProfileFragment extends Fragment {

    private TextView tvTitle, txtEmail, txtFullName, txtGender, txtBirthDate, txtPhone, txtInterest;
    private TextView tvEmailValue, tvFullNameValue, tvGenderValue, tvBirthDateValue, tvPhoneValue, tvInterestValue;
    private ImageView imgAvatar;
    private Button btnLogout;

    private DatabaseReference databaseRef;
    private FirebaseUser currentUser;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_activity_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TextView label
        tvTitle = view.findViewById(R.id.tvTitle);
        txtEmail = view.findViewById(R.id.txtemail);
        txtFullName = view.findViewById(R.id.txtFullName);
        txtGender = view.findViewById(R.id.txtGender);
        txtBirthDate = view.findViewById(R.id.txtBirthDate);
        txtPhone = view.findViewById(R.id.txtPhone);
        txtInterest = view.findViewById(R.id.txtInterest);

        // TextView value
        tvEmailValue = view.findViewById(R.id.tvEmail);
        tvFullNameValue = view.findViewById(R.id.tvFullName);
        tvGenderValue = view.findViewById(R.id.tvGender);
        tvBirthDateValue = view.findViewById(R.id.tvBirthDate);
        tvPhoneValue = view.findViewById(R.id.tvPhone);
        tvInterestValue = view.findViewById(R.id.tvInterest);

        imgAvatar = view.findViewById(R.id.imgAvatar);

        // Set text đa ngôn ngữ
        tvTitle.setText(getString(R.string.profile_title));
        txtEmail.setText(getString(R.string.email_label));
        txtFullName.setText(getString(R.string.fullname_label));
        txtGender.setText(getString(R.string.gender_label));
        txtBirthDate.setText(getString(R.string.birthdate_label));
        txtPhone.setText(getString(R.string.phone_label));
        txtInterest.setText(getString(R.string.interest_label));

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), getString(R.string.please_login_again), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
            return;
        }

        String userId = currentUser.getUid();
        databaseRef = FirebaseDatabase
                .getInstance("https://nt118q14-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Users")
                .child(userId);

        tvEmailValue.setText(" " + currentUser.getEmail());

        loadUserInfo();

        imgAvatar.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(getString(R.string.choose_new_avatar_title));
            builder.setMessage(getString(R.string.choose_new_avatar_message));
            builder.setPositiveButton(getString(R.string.choose_image), (dialog, which) -> openImagePicker());
            builder.setNegativeButton(getString(R.string.cancel), null);
            builder.show();
        });
    }

    private void loadUserInfo() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(getContext(), getString(R.string.user_data_not_found), Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String birthDate = snapshot.child("birthDate").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String interest = snapshot.child("interest").getValue(String.class);
                    String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);

                    tvFullNameValue.setText(" " + (fullName != null ? Encryption.decrypt(fullName) : getString(R.string.user_data_not_found)));

                    if (gender != null) {
                        String genderDecrypted = Encryption.decrypt(gender).trim().toLowerCase();
                        if (genderDecrypted.equals("nam") || genderDecrypted.equals("male")) {
                            tvGenderValue.setText(" " + getString(R.string.gender_male));
                        } else if (genderDecrypted.equals("nữ") || genderDecrypted.equals("female")) {
                            tvGenderValue.setText(" " + getString(R.string.gender_female));
                        } else {
                            tvGenderValue.setText(" " + getString(R.string.user_data_not_found));
                        }
                    } else {
                        tvGenderValue.setText(" " + getString(R.string.user_data_not_found));
                    }

                    tvBirthDateValue.setText(" " + (birthDate != null ? Encryption.decrypt(birthDate) : getString(R.string.user_data_not_found)));
                    tvPhoneValue.setText(" " + (phone != null ? Encryption.decrypt(phone) : getString(R.string.user_data_not_found)));
                    tvInterestValue.setText(" " + (interest != null ? Encryption.decrypt(interest) : getString(R.string.user_data_not_found)));

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
                            imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                        }
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), getString(R.string.user_data_load_fail), Toast.LENGTH_SHORT).show();
                    imgAvatar.setImageResource(R.drawable.ic_logo_uit);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), getString(R.string.database_error, error.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
                Bitmap circleBitmap = getCircularBitmap(resizedBitmap);

                imgAvatar.setImageBitmap(circleBitmap);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                uploadImageToFirebase(encodedImage);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), getString(R.string.select_image_fail), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadImageToFirebase(String encodedImage) {
        if (currentUser == null) return;

        databaseRef.child("avatarBase64").setValue(encodedImage)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), getString(R.string.update_avatar_success), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.update_avatar_fail), Toast.LENGTH_SHORT).show());
    }

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
