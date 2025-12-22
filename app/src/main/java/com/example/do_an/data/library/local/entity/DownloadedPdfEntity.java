package com.example.do_an.data.library.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloaded_pdfs")
public class DownloadedPdfEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;
    @NonNull
    public String storyDocumentId;
    public String fileName;
    public String localFilePath;
    public String pdfUrl;
    public String author;
    public String coverImageUrl;
    public boolean isCache = false;

    public DownloadedPdfEntity() {}
}