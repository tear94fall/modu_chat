package com.example.modumessenger.RoomDatabase.Dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.modumessenger.RoomDatabase.Entity.ChatRoomEntity;

import java.util.List;

@Dao
public interface ChatRoomDao {

    @Insert
    void insert(ChatRoomEntity chatRoomEntity);

    @Update
    void update(ChatRoomEntity chatRoomEntity);

    @Delete
    void delete(ChatRoomEntity chatRoomEntity);

    @Query("SELECT * FROM chat_room")
    LiveData<List<ChatRoomEntity>> getAll();

    @Query("SELECT * FROM chat_room WHERE id = :id")
    ChatRoomEntity findById(Long id);

    @Query("SELECT * FROM chat_room WHERE room_id = :room_id")
    ChatRoomEntity findByRoomId(String room_id);
}
