package com.example.do_an.home;

public class Book {
    private String id; // ⭐ Đã thêm: Trường để lưu Document ID từ Firestore ⭐
    private String name;
    private String author;
    private String link;
    private String imageUrl;
    private String category;

    public Book() {}

    // ⭐ Đã thêm: Getter và Setter cho ID ⭐
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLink() {
        return link;
    }
    public void setLink(String link) {
        this.link = link;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}