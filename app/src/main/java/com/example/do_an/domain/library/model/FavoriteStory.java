package com.example.do_an.domain.library.model;

public class FavoriteStory {
    private String storyId;
    private String title;
    private String author;
    private String category;
    private String imageUrl;
    private String readUrl;

    public FavoriteStory() {
    }

    public String getStoryId() {
        return storyId;
    }

    public void setStoryId(String id) {
        this.storyId = id;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getReadUrl() { return readUrl; }
    public void setReadUrl(String readUrl) { this.readUrl = readUrl; }
}