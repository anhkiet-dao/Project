package com.example.do_an.history.item;

public class HistoryItem {
    public String title, author, startTime, endTime;

    public HistoryItem(String title, String author, String startTime, String endTime) {
        this.title = title;
        this.author = author;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}