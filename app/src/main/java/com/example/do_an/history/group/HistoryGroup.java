package com.example.do_an.history.group;

import com.example.do_an.history.item.HistoryItem;

import java.util.ArrayList;

public class HistoryGroup {

    public String date;
    public ArrayList<HistoryItem> items;

    public HistoryGroup(String date, ArrayList<HistoryItem> items) {
        this.date = date;
        this.items = items;
    }
}