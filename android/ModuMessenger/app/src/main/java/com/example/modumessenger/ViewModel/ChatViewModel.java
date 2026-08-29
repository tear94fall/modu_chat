package com.example.modumessenger.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Global.SingleLiveEvent;
import com.example.modumessenger.Global.socket.ConnectionState;
import com.example.modumessenger.Repository.BannerEvent;
import com.example.modumessenger.Repository.ChatRepository;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatViewModel extends ViewModel {

    private static final DateTimeFormatter CHAT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChatRepository repository;
    private final String roomId;
    private final String myUserId;

    public ChatViewModel(ChatRepository repository, String roomId, String myUserId) {
        this.repository = repository;
        this.roomId = roomId;
        this.myUserId = myUserId;
        repository.openRoom(roomId);
    }

    public LiveData<List<ChatBubble>> getChats() {
        return repository.getActiveRoomChats();
    }

    public LiveData<ConnectionState> getConnectionState() {
        return repository.getConnectionState();
    }

    public SingleLiveEvent<BannerEvent> getBanner() {
        return repository.getBanner();
    }

    public String getRoomId() {
        return roomId;
    }

    public void loadInitial(int size) {
        repository.loadInitialChats(roomId, size);
    }

    public void loadPrev(String oldestChatId, int size) {
        repository.loadPrevChats(roomId, oldestChatId, size);
    }

    public boolean sendText(String message) {
        return send(message, ChatType.CHAT_TYPE_TEXT);
    }

    public boolean sendImage(String filename) {
        return send(filename, ChatType.CHAT_TYPE_IMAGE);
    }

    private boolean send(String message, int chatType) {
        ChatDto dto = new ChatDto();
        dto.setRoomId(roomId);
        dto.setMessage(message);
        dto.setSender(myUserId);
        dto.setChatTime(LocalDateTime.now().format(CHAT_TIME_FORMAT));
        dto.setChatType(chatType);

        return repository.sendChat(dto);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 화면이 실제로 사라질 때만 호출된다. 회전 시에는 호출되지 않는다.
        repository.closeRoom(roomId);
    }
}
