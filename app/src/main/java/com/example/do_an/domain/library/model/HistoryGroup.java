package com.example.do_an.domain.library.model;

import java.util.ArrayList;

public class HistoryGroup {
    private final String date;
    private final ArrayList<HistoryItem> items;

    public HistoryGroup(String date, ArrayList<HistoryItem> items) {
        this.date = date;
        this.items = items;
    }

    public String getDate() {
        return date;
    }

    public ArrayList<HistoryItem> getItems() {
        return items;
    }
}
