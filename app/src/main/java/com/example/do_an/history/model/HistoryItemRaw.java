package com.example.do_an.history.model;

public class HistoryItemRaw {

    public String startTime;
    public String endTime;
    public String author;
    public String episodeTitle;
    public String title;

    public HistoryItemRaw(String title,
                          String author,
                          String episodeTitle,
                          String start,
                          String end) {
        this.title = title;
        this.author = author;
        this.episodeTitle = episodeTitle;
        this.startTime = start;
        this.endTime = end;
    }
}
