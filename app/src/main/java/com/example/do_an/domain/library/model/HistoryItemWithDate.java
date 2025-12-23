package com.example.do_an.domain.library.model;

import java.util.Date;

public class HistoryItemWithDate {
    private final HistoryItem item;
    private final Date date;

    public HistoryItemWithDate(HistoryItem item, Date date) {
        this.item = item;
        this.date = date;
    }

    public HistoryItem getItem() {
        return item;
    }

    public Date getDate() {
        return date;
    }
}
