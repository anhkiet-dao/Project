package com.example.do_an.history;

import android.util.Log;

import com.example.do_an.core.encryption.Encryption;
import com.example.do_an.core.constants.FirebaseCollectionPaths;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nullable;

public class HistoryManager {

    private static final String TAG = "HistoryManager";
    private String currentHistoryKey;

    public HistoryManager(Object context) {}

    public void saveStartReadingHistory(@Nullable String userEmail,
                                        @Nullable String storyId,
                                        @Nullable String mainStoryTitle,
                                        String currentTitle,
                                        String author) {

        if (userEmail == null || storyId == null) return;

        userEmail = userEmail.replace(".", "_");

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

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseCollectionPaths.HISTORY)
                .child(userEmail)
                .push();

        currentHistoryKey = dbRef.getKey();
        dbRef.setValue(historyData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian bắt đầu đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian bắt đầu", e));
    }

    public void saveEndReadingHistory(String userEmail) {
        if (userEmail == null || currentHistoryKey == null) return;

        userEmail = userEmail.replace(".", "_");

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String endTime = sdf.format(new Date());

        String encryptedEndTime = Encryption.encrypt(endTime);

        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference(FirebaseCollectionPaths.HISTORY)
                .child(userEmail)
                .child(currentHistoryKey)
                .child("endTime");

        dbRef.setValue(encryptedEndTime)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Đã lưu thời gian kết thúc đọc"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Lỗi lưu thời gian kết thúc", e));
    }
}