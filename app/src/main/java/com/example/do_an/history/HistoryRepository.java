package com.example.do_an.history;

import com.example.do_an.application.Encryption;
import com.example.do_an.history.group.HistoryGroup;
import com.example.do_an.history.item.HistoryItem;
import com.example.do_an.history.model.HistoryItemRaw;
import com.google.firebase.database.DataSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HistoryRepository {

    public List<HistoryGroup> processSnapshot(DataSnapshot snapshot) {
        List<HistoryItemRaw> rawItems = parseSnapshot(snapshot);
        Map<String, List<HistoryItem>> grouped = groupItemsByDate(rawItems);
        return sortAndConvert(grouped);
    }

    private List<HistoryItemRaw> parseSnapshot(DataSnapshot snapshot) {
        List<HistoryItemRaw> list = new ArrayList<>();

        for (DataSnapshot child : snapshot.getChildren()) {
            String title = decrypt(child, "title");
            String author = decrypt(child, "author");
            String episodeTitle = decrypt(child, "episodeTitle");
            String start = decrypt(child, "startTime");
            String end = decrypt(child, "endTime");

            if (start == null || end == null) {
                continue;
            }

            list.add(new HistoryItemRaw(title, author, episodeTitle, start, end));
        }

        return list;
    }

    private Map<String, List<HistoryItem>> groupItemsByDate(List<HistoryItemRaw> rawList) {
        Map<String, List<HistoryItem>> map = new HashMap<>();

        SimpleDateFormat sdfFull = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

        for (HistoryItemRaw raw : rawList) {
            try {
                Date start = sdfFull.parse(raw.startTime);
                Date end = sdfFull.parse(raw.endTime);

                if (start == null || end == null) {
                    continue;
                }

                String dateKey = sdfDate.format(start);
                String displayTitle = createDisplayTitle(raw);

                HistoryItem item = new HistoryItem(
                        displayTitle,
                        raw.author != null ? raw.author : "—",
                        sdfTime.format(start),
                        sdfTime.format(end)
                );

                map.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(item);

            } catch (Exception ignored) {
            }
        }

        return map;
    }

    private String createDisplayTitle(HistoryItemRaw raw) {
        return (raw.episodeTitle != null && !raw.episodeTitle.isEmpty())
                ? raw.title + " - " + raw.episodeTitle
                : raw.title;
    }


    private String decrypt(DataSnapshot snap, String key) {
        return Encryption.decrypt(snap.child(key).getValue(String.class));
    }

    private List<HistoryGroup> sortAndConvert(Map<String, List<HistoryItem>> map) {
        List<String> dates = new ArrayList<>(map.keySet());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        dates.sort((d1, d2) -> {
            try {
                return Objects.requireNonNull(sdf.parse(d2)).compareTo(sdf.parse(d1));
            } catch (Exception e) {
                return 0;
            }
        });

        List<HistoryGroup> groups = new ArrayList<>();
        for (String date : dates) {
            groups.add(new HistoryGroup(date, (ArrayList<HistoryItem>) map.get(date)));
        }

        return groups;
    }
}
