package com.example.do_an.domain.reading.repository;

import android.util.Log;

import com.example.do_an.core.utils.Encryption;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private String currentHistoryKey;
    public HistoryManager(Object context) {
    }

    public void saveStartReadingHistory(String userEmail, String storyId, String mainStoryTitle,
                                        String currentTitle, String author, String imageUrl) { // ⬅️ THÊM imageUrl
        if (userEmail == null || storyId == null) return;

        String currentEpisodeTitle = (currentTitle.equals(mainStoryTitle)) ? "" : currentTitle;
        String titleForHistory = (mainStoryTitle != null) ? mainStoryTitle : currentTitle;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String startTime = sdf.format(new Date());

        HashMap<String, Object> historyData = new HashMap<>();
        historyData.put("title", Encryption.encrypt(titleForHistory));
        historyData.put("author", Encryption.encrypt(author));
        historyData.put("episodeTitle", Encryption.encrypt(currentEpisodeTitle));
        historyData.put("startTime", Encryption.encrypt(startTime));
        historyData.put("storyId", Encryption.encrypt(storyId));
        historyData.put("imageUrl", Encryption.encrypt(imageUrl != null ? imageUrl : "")); // ⬅️ LƯU imageUrl


        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(userEmail.replace(".", "_"))
                .push();
        currentHistoryKey = dbRef.getKey(); // Lưu key để dùng cho thời gian kết thúc
        dbRef.setValue(historyData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian bắt đầu đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian bắt đầu", e));
    }

    public void saveEndReadingHistory(String userEmail) {
        if (userEmail == null || currentHistoryKey == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String endTime = sdf.format(new Date());

        String encryptedEndTime = Encryption.encrypt(endTime);

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference("History")
                .child(userEmail.replace(".", "_"))
                .child(currentHistoryKey)
                .child("endTime");

        dbRef.setValue(encryptedEndTime)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian kết thúc đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian kết thúc", e));
    }
}