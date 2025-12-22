package com.example.do_an.data.reading.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import com.example.do_an.data.reading.local.entity.CachePdfEntity;

import java.util.List;

@Dao
public interface CachePdfDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CachePdfEntity cachePdf);

    @Query("SELECT * FROM cache_pdfs WHERE storyDocumentId = :storyId LIMIT 1")
    CachePdfEntity getCacheByStoryId(String storyId);

    @Delete
    void delete(CachePdfEntity cachePdf);

    @Query("DELETE FROM cache_pdfs")
    void clearAllCache();

    @Query("SELECT * FROM cache_pdfs WHERE lastAccessedTime < :timestamp")
    List<CachePdfEntity> getOldCache(long timestamp);
}