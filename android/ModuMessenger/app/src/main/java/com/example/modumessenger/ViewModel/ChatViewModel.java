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

    /**
     * 내가 방금 보낸 메시지가 있어 화면을 바닥으로 강제로 내려야 한다는 표시.
     * 액티비티는 getChats() 관찰 콜백에서 한 번 소비하고 지운다 — 클릭 핸들러에서
     * 곧바로 스크롤을 부르면 어댑터가 아직 갱신 전이라 아무 효과가 없기 때문에,
     * 실제 갱신이 도착하는 시점(관찰 콜백)까지 의도를 들고 있는 용도다.
     */
    private boolean pendingScrollToBottom = false;

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

    public boolean resendFailed(ChatBubble chatBubble) {
        if (chatBubble == null || chatBubble.getId() == null) return false;
        return repository.resendFailedChat(chatBubble.getId());
    }

    public void deleteFailed(ChatBubble chatBubble) {
        if (chatBubble == null || chatBubble.getId() == null) return;
        repository.deleteFailedChat(chatBubble.getId());
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

        boolean sent = repository.sendChat(dto);
        // 성공이든 실패든 말풍선(정상 또는 재전송 가능한 실패 말풍선)이 하나 추가되니
        // 두 경우 다 바닥으로 내려 보여준다.
        pendingScrollToBottom = true;
        return sent;
    }

    /**
     * 대기 중인 강제 스크롤 표시를 한 번 읽고 지운다. 관찰 콜백에서 딱 한 번만 소비해야
     * 그 다음에 도착하는(내 전송과 무관한) 갱신에서 잘못 다시 스크롤하지 않는다.
     */
    public boolean consumePendingScrollToBottom() {
        boolean value = pendingScrollToBottom;
        pendingScrollToBottom = false;
        return value;
    }

    /**
     * 맨 아래로 이동 버튼을 띄울지 결정한다: 바닥이 아닐 때 다른 사람이 보낸 메시지가
     * 도착한 경우에만 띄운다. 스크롤 리스너 안에 흩어놓지 않고 한 곳에 모아 테스트한다.
     */
    public static boolean shouldShowJumpToBottom(boolean wasAtBottom, String lastMessageSenderId, String myUserId) {
        if (wasAtBottom) return false;
        if (lastMessageSenderId == null) return false;
        return !lastMessageSenderId.equals(myUserId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // 화면이 실제로 사라질 때만 호출된다. 회전 시에는 호출되지 않는다.
        repository.closeRoom(roomId);
    }
}
