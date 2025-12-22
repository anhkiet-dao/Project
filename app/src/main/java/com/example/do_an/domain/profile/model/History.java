package com.example.do_an.domain.profile.model;

public class History {

    private String startTime;
    private String endTime;
    private String title;
    private String author;
    private String episodeTitle;
    private String storyId;

    public History() {} // constructor rỗng bắt buộc để Firebase có thể map

    // --- Getters & Setters ---
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getEpisodeTitle() { return episodeTitle; }
    public void setEpisodeTitle(String episodeTitle) { this.episodeTitle = episodeTitle; }

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }
}
