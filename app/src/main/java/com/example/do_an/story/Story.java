package com.example.do_an.story;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Story {
    @Exclude
    private String id;
    private String thumbnail;
    private String title;
    private String author;
    private String year;
    private String genre;

    public Story() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("anhbia")
    public String getThumbnail() { return thumbnail; }
    @PropertyName("anhbia")
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    @PropertyName("tenTruyen")
    public String getTitle() { return title; }
    @PropertyName("tenTruyen")
    public void setTitle(String title) { this.title = title; }

    @PropertyName("tacGia")
    public String getAuthor() { return author; }
    @PropertyName("tacGia")
    public void setAuthor(String author) { this.author = author; }

    @PropertyName("namPhatHanh")
    public String getYear() { return year; }
    @PropertyName("namPhatHanh")
    public void setYear(String year) { this.year = year; }

    @PropertyName("theLoai")
    public String getGenre() { return genre; }
    @PropertyName("theLoai")
    public void setGenre(String genre) { this.genre = genre; }
}
