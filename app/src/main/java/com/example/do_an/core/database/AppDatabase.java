package com.example.do_an.core.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.do_an.data.reading.local.dao.CachePdfDao;
import com.example.do_an.data.reading.local.entity.CachePdfEntity;
import com.example.do_an.data.library.local.dao.DownloadedPdfDao;
import com.example.do_an.data.library.local.entity.DownloadedPdfEntity;

@Database(entities = {DownloadedPdfEntity.class, CachePdfEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract DownloadedPdfDao downloadedPdfDao();
    public abstract CachePdfDao cachePdfDao();

    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "MyStoryApp_DB";

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}