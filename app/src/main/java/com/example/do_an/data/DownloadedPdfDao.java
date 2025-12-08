package com.example.do_an.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadedPdfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadedPdfEntity pdf);

    @Query("SELECT * FROM downloaded_pdfs WHERE storyDocumentId = :storyId LIMIT 1")
    DownloadedPdfEntity getPdfByStoryId(String storyId);

    @Query("SELECT * FROM downloaded_pdfs WHERE pdfUrl = :pdfUrl LIMIT 1")
    DownloadedPdfEntity getPdfByUrl(String pdfUrl);

    @Delete
    void delete(DownloadedPdfEntity pdf);
    @Query("SELECT * FROM downloaded_pdfs WHERE isCache = 0")
    List<DownloadedPdfEntity> getAllPdfs();

    @Query("SELECT * FROM downloaded_pdfs WHERE localFilePath = :path LIMIT 1")
    DownloadedPdfEntity getPdfByFilePath(String path);

    @Query("SELECT * FROM downloaded_pdfs WHERE fileName = :fileName LIMIT 1")
    DownloadedPdfEntity getPdfByFileName(String fileName);
}