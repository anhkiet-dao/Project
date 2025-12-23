package com.example.do_an.presentation.library.history.util;

import com.example.do_an.core.utils.Encryption;
import com.example.do_an.domain.library.model.HistoryItem;
import com.example.do_an.domain.library.model.HistoryItemWithDate;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HistoryDataProcessor {

    private final SimpleDateFormat sdfFull;
    private final SimpleDateFormat sdfDay;
    private final SimpleDateFormat sdfTime;

    public HistoryDataProcessor() {
        this.sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        this.sdfDay = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        this.sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    }

    public ArrayList<HistoryItemWithDate> parseHistoryItems(DataSnapshot snapshot) {
        ArrayList<HistoryItemWithDate> items = new ArrayList<>();

        for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
            HistoryItemWithDate item = parseHistoryItem(itemSnapshot);
            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    private HistoryItemWithDate parseHistoryItem(DataSnapshot itemSnapshot) {
        try {
            String title = Encryption.decrypt(itemSnapshot.child("title").getValue(String.class));
            String author = Encryption.decrypt(itemSnapshot.child("author").getValue(String.class));
            String episodeTitle = Encryption.decrypt(itemSnapshot.child("episodeTitle").getValue(String.class));
            String startTimeStr = Encryption.decrypt(itemSnapshot.child("startTime").getValue(String.class));
            String endTimeStr = Encryption.decrypt(itemSnapshot.child("endTime").getValue(String.class));
            String imageUrl = Encryption.decrypt(itemSnapshot.child("imageUrl").getValue(String.class));

            Date startDate = sdfFull.parse(startTimeStr);
            if (startDate == null)
                return null;

            Date endDate = null;
            if (endTimeStr != null) {
                try {
                    endDate = sdfFull.parse(endTimeStr);
                } catch (Exception ignored) {
                }
            }

            String displayTitle = (episodeTitle != null && !episodeTitle.isEmpty())
                    ? title + " - " + episodeTitle
                    : title;

            HistoryItem item = new HistoryItem(
                    displayTitle,
                    author != null ? author : "—",
                    sdfTime.format(startDate),
                    endDate != null ? sdfTime.format(endDate) : "—",
                    imageUrl);

            return new HistoryItemWithDate(item, startDate);
        } catch (Exception e) {
            return null;
        }
    }

    public void sortByDateDescending(ArrayList<HistoryItemWithDate> items) {
        items.sort((i1, i2) -> i2.getDate().compareTo(i1.getDate()));
    }

    public Map<String, ArrayList<HistoryItem>> groupItemsByDate(ArrayList<HistoryItemWithDate> items) {
        Map<String, ArrayList<HistoryItem>> mapDay = new HashMap<>();

        for (HistoryItemWithDate itemWithDate : items) {
            String dateKey = sdfDay.format(itemWithDate.getDate());
            if (!mapDay.containsKey(dateKey)) {
                mapDay.put(dateKey, new ArrayList<>());
            }
            if (mapDay.get(dateKey).size() < 10) {
                mapDay.get(dateKey).add(itemWithDate.getItem());
            }
        }

        return mapDay;
    }

    public ArrayList<String> sortDates(Iterable<String> dates) {
        ArrayList<String> sortedDates = new ArrayList<>();
        for (String date : dates) {
            sortedDates.add(date);
        }

        sortedDates.sort((d1, d2) -> {
            try {
                return sdfDay.parse(d2).compareTo(sdfDay.parse(d1));
            } catch (Exception e) {
                return 0;
            }
        });

        return sortedDates;
    }
}
