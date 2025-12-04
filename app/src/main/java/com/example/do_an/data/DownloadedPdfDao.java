package com.example.do_an.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadedPdfDao {

    // Thêm (Insert) hoặc thay thế (Replace) nếu đã tồn tại
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DownloadedPdfEntity pdf);

    // Lấy thông tin PDF đã tải xuống dựa trên ID truyện/document
    @Query("SELECT * FROM downloaded_pdfs WHERE storyDocumentId = :storyId LIMIT 1")
    DownloadedPdfEntity getPdfByStoryId(String storyId);

    // Xóa một Entity cụ thể (dùng khi file bị mất hoặc người dùng muốn xóa)
    @Delete
    void delete(DownloadedPdfEntity pdf);

    @Query("SELECT * FROM downloaded_pdfs")
    List<DownloadedPdfEntity> getAllPdfs();

    @Query("SELECT * FROM downloaded_pdfs WHERE localFilePath = :path LIMIT 1")
    DownloadedPdfEntity getPdfByFilePath(String path);

    // BỔ SUNG: Truy vấn Entity dựa trên tên file (để lấy thông tin tác giả/metadata)
    @Query("SELECT * FROM downloaded_pdfs WHERE fileName = :fileName LIMIT 1")
    DownloadedPdfEntity getPdfByFileName(String fileName);
}