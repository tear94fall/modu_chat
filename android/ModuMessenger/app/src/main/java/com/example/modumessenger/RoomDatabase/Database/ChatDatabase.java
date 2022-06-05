package com.example.modumessenger.RoomDatabase.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.modumessenger.RoomDatabase.Dao.ChatDao;
import com.example.modumessenger.RoomDatabase.Entity.ChatEntity;

@Database(entities = {ChatEntity.class},version = 2)
public abstract class ChatDatabase extends RoomDatabase {

    private static ChatDatabase instance;
    private static String DATABASE_NAME = "modu-chat-db";
    public abstract ChatDao chatDao();

    public static ChatDatabase getInstance(Context context) {
        if(instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), ChatDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }

        return instance;
    }

    public static void deleteInstance() { instance = null; }
}
