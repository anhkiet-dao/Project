package com.example.do_an.application;

// package com.example.do_an.models;

public class NoteModel {
    public String userId; // Email người dùng
    public String noteContextId; // ID ngữ cảnh (Story ID + Title)
    public int pageNumber; // Số trang
    public String content; // Nội dung ghi chú
    public long timestamp; // Thời gian cập nhật

    public NoteModel() {
        // Cần thiết cho Firebase
    }

    public NoteModel(String userId, String noteContextId, int pageNumber, String content, long timestamp) {
        this.userId = userId;
        this.noteContextId = noteContextId;
        this.pageNumber = pageNumber;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Bạn có thể thêm Getters và Setters nếu cần thiết, nhưng Firebase hoạt động tốt với các public field
}