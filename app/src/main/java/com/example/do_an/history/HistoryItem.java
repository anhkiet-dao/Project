package com.example.do_an.history;

public class HistoryItem {
    String title, author, startTime, endTime;

    public HistoryItem(String title, String author, String startTime, String endTime) {
        this.title = title;
        this.author = author;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}