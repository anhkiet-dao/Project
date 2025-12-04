package com.example.do_an.history;

import java.util.ArrayList;

public class HistoryGroup {

    String date;
    ArrayList<HistoryItem> items;

    public HistoryGroup(String date, ArrayList<HistoryItem> items) {
        this.date = date;
        this.items = items;
    }
}