package com.example.do_an.data.reading.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cache_pdfs")
public class CachePdfEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String storyDocumentId;
    public String fileName;
    public String localFilePath;
    public String pdfUrl;
    public long lastAccessedTime;

    public CachePdfEntity() {
        this.lastAccessedTime = System.currentTimeMillis();
    }
}
