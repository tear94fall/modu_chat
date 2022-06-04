package com.example.modumessenger.RoomDatabase.Database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.modumessenger.RoomDatabase.Dao.ChatRoomDao;
import com.example.modumessenger.RoomDatabase.Entity.ChatRoomEntity;

@Database(entities = {ChatRoomEntity.class}, version =  1)
public abstract class ChatRoomDatabase extends RoomDatabase {

    private static ChatRoomDatabase instance;
    private static String DATABASE_NAME = "modu-chat-db";
    public abstract ChatRoomDao chatRoomDao();

    public static ChatRoomDatabase getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(context.getApplicationContext(), ChatRoomDatabase.class, DATABASE_NAME)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return  instance;
    }

    public static void deleteInstance() {
        instance = null;
    }
}