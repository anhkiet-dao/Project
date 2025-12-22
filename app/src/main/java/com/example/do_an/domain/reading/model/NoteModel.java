package com.example.do_an.domain.reading.model;

// package com.example.do_an.models;

public class NoteModel {
    public String userId;
    public String noteContextId;
    public int pageNumber;
    public String content;
    public long timestamp;

    public NoteModel() {
    }

    public NoteModel(String userId, String noteContextId, int pageNumber, String content, long timestamp) {
        this.userId = userId;
        this.noteContextId = noteContextId;
        this.pageNumber = pageNumber;
        this.content = content;
        this.timestamp = timestamp;
    }

}