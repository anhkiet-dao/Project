package com.example.do_an.Series;

public class Series {
    private String id;       // Document ID trong Firestore
    private String name;
    private String link;

    // Constructor rỗng bắt buộc cho Firestore
    public Series() {}

    public Series(String name, String link) {
        this.name = name;
        this.link = link;
    }

    // Getter & Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    @Override
    public String toString() {
        return "Series{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", link='" + link + '\'' +
                '}';
    }
}