package com.example.do_an.data.library.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;

import java.util.List;

@Dao
public interface DownloadedPdfDao {

    @Query("SELECT * FROM downloaded_pdfs ORDER BY id DESC")
    List<DownloadedPdfEntity> getAllPdfs(); // ⬅️ ĐÃ BỎ isCache = 0

    @Query("SELECT * FROM downloaded_pdfs WHERE storyDocumentId = :storyId AND isCache = 0 LIMIT 1")
    DownloadedPdfEntity getPdfByStoryId(String storyId);

    @Query("SELECT * FROM downloaded_pdfs WHERE fileName = :fileName AND isCache = 0 LIMIT 1")
    DownloadedPdfEntity getPdfByFileName(String fileName);

    @Query("SELECT * FROM downloaded_pdfs WHERE localFilePath = :filePath AND isCache = 0 LIMIT 1")
    DownloadedPdfEntity getPdfByFilePath(String filePath);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadedPdfEntity pdf);

    @Update
    void update(DownloadedPdfEntity pdf);

    @Delete
    void delete(DownloadedPdfEntity pdf);
}