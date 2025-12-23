package com.example.do_an.domain.library.model;

public class HistoryItem {
    private final String title;
    private final String author;
    private final String startTime;
    private final String endTime;
    private final String imageUrl;

    public HistoryItem(String title, String author, String startTime, String endTime, String imageUrl) {
        this.title = title;
        this.author = author;
        this.startTime = startTime;
        this.endTime = endTime;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
