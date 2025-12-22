package com.example.do_an.domain.library.model;

import com.google.firebase.firestore.Exclude;

public class Story {
    private String anhbia, tenTruyen, tacGia, namPhatHanh, theLoai;

    @Exclude // Dùng @Exclude để Firestore không lưu trường này khi ghi
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Story() {}

    public String getAnhBia() { return anhbia; }
    public String getTenTruyen() { return tenTruyen; }
    public String getTacGia() { return tacGia; }
    public String getNamPhatHanh() { return namPhatHanh; }
    public String getTheLoai() { return theLoai; }
}
