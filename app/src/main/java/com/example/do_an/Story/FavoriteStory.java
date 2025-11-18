package com.example.do_an.Story;

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
    public String getAuthor() { return author; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public String getReadUrl() { return readUrl; }
}
