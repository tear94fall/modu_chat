package com.example.modumessenger.RoomDatabase.Dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.modumessenger.RoomDatabase.Entity.ChatEntity;

import java.util.List;

@Dao
public interface ChatDao {

    @Insert
    void insert(ChatEntity chatEntity);

    @Update
    void update(ChatEntity chatEntity);

    @Delete
    void delete(ChatEntity chatEntity);

    @Query("SELECT * FROM chat")
    LiveData<List<ChatEntity>> getAll();

    @Query("SELECT * FROM chat WHERE id = :id")
    ChatEntity findById(Long id);

    @Query("SELECT * FROM chat WHERE room_id = :room_id")
    ChatEntity findByRoomId(String room_id);
};
