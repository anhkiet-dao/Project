package com.example.do_an.library.favorite;

public class FavoriteStory {
    private String storyId;
    private String title;
    private String author;
    private String category;
    private String imageUrl;
    private String readUrl;

    public String getStoryId() { return storyId; }

    public void setId(String id) {
        this.storyId = storyId;
    }

    public FavoriteStory() {
        // BẮT BUỘC cho Firebase
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
