package com.example.modumessenger.Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Global.socket.WebSocketManager;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.entity.ChatRoom;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatRepositoryTest {

    private static final String ACTIVE_ROOM = "room-active";
    private static final String OTHER_ROOM = "room-other";
    private static final String ME = "me";
    private static final String OTHER = "someone-else";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ChatRepository repository;

    private static ChatRoom room(String roomId) {
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setRoomId(roomId);
        chatRoom.setRoomName(roomId);
        return chatRoom;
    }

    private static ChatDto chat(long id, String roomId, String sender, String message) {
        ChatDto dto = new ChatDto();
        dto.setId(id);
        dto.setRoomId(roomId);
        dto.setSender(sender);
        dto.setMessage(message);
        dto.setChatTime("2026-08-26 10:00:00");
        dto.setChatType(0);
        return dto;
    }

    @Before
    public void setUp() {
        repository = new ChatRepository(
                mock(WebSocketManager.class),
                mock(RetrofitChatAPI.class),
                mock(RetrofitChatRoomAPI.class));

        repository.setIdentity(ME, "1");
        repository.setChatRooms(new ArrayList<>(Arrays.asList(room(OTHER_ROOM), room(ACTIVE_ROOM))));
        repository.openRoom(ACTIVE_ROOM);
    }

    private List<ChatBubble> bubbles() {
        List<ChatBubble> value = repository.getActiveRoomChats().getValue();
        assertNotNull(value);
        return value;
    }

    private List<ChatRoom> rooms() {
        List<ChatRoom> value = repository.getChatRooms().getValue();
        assertNotNull(value);
        return value;
    }

    @Test
    public void myMessageInActiveRoom_addsBubble_noBanner() {
        repository.handleChat(chat(1L, ACTIVE_ROOM, ME, "안녕"), false);

        assertEquals(1, bubbles().size());
        assertEquals("안녕", bubbles().get(0).getChatMsg());
        assertNull("자기 메시지에는 배너가 뜨면 안 된다", repository.getBanner().getValue());
    }

    @Test
    public void othersMessageInActiveRoom_addsBubble_noBanner() {
        repository.handleChat(chat(1L, ACTIVE_ROOM, OTHER, "왔어?"), false);

        assertEquals(1, bubbles().size());
        assertNull("보고 있는 방은 배너 대상이 아니다", repository.getBanner().getValue());
    }

    @Test
    public void otherRoomMessage_noBubble_updatesRoomList_showsBanner() {
        repository.handleChat(chat(7L, OTHER_ROOM, OTHER, "다른 방"), false);

        assertEquals("활성 방이 아니면 말풍선을 추가하지 않는다", 0, bubbles().size());

        assertEquals(OTHER_ROOM, rooms().get(0).getRoomId());
        assertEquals("다른 방", rooms().get(0).getLastChatMsg());
        assertEquals("7", rooms().get(0).getLastChatId());

        assertNotNull(repository.getBanner().getValue());
        assertEquals(OTHER_ROOM, repository.getBanner().getValue().getRoomId());
    }

    @Test
    public void gapRecoveryInOtherRoom_showsNoBanner() {
        // 같은 입력을 isGapRecovery=false 로 주면 배너가 뜨는 조건이다.
        // 플래그만으로 배너가 억제되는지를 본다.
        repository.handleChat(chat(9L, OTHER_ROOM, OTHER, "복구분"), true);

        assertNull("갭 복구분은 FCM 으로 이미 알림을 받았으므로 배너를 띄우지 않는다",
                repository.getBanner().getValue());
        assertEquals("방 목록은 갱신되어야 한다", OTHER_ROOM, rooms().get(0).getRoomId());
        assertEquals("활성 방이 아니므로 말풍선은 없다", 0, bubbles().size());
    }

    @Test
    public void gapRecoveryInActiveRoom_addsBubble_noBanner() {
        repository.handleChat(chat(9L, ACTIVE_ROOM, OTHER, "복구분"), true);

        assertEquals(1, bubbles().size());
        assertEquals("복구분", bubbles().get(0).getChatMsg());
        assertNull(repository.getBanner().getValue());
    }

    @Test
    public void sameChatIdTwice_isDeduplicated() {
        ChatDto dto = chat(1L, ACTIVE_ROOM, OTHER, "중복");

        repository.handleChat(dto, false);
        repository.handleChat(dto, true);

        assertEquals("같은 chatId 는 한 번만 남아야 한다", 1, bubbles().size());
    }

    @Test
    public void bubbleOrderFollowsArrival() {
        repository.handleChat(chat(1L, ACTIVE_ROOM, OTHER, "첫번째"), false);
        repository.handleChat(chat(2L, ACTIVE_ROOM, ME, "두번째"), false);

        assertEquals(2, bubbles().size());
        assertEquals("첫번째", bubbles().get(0).getChatMsg());
        assertEquals("두번째", bubbles().get(1).getChatMsg());
    }

    @Test
    public void closeRoom_clearsActiveRoomState() {
        repository.handleChat(chat(1L, ACTIVE_ROOM, OTHER, "안녕"), false);
        repository.closeRoom(ACTIVE_ROOM);

        assertEquals(0, bubbles().size());
        assertNull(repository.getActiveRoomId());
    }

    @Test
    public void chatWithoutId_isIgnored() {
        ChatDto dto = chat(1L, ACTIVE_ROOM, OTHER, "id 없음");
        dto.setId(null);

        repository.handleChat(dto, false);

        assertEquals(0, bubbles().size());
    }

    @Test
    public void myMessageInOtherRoom_showsNoBanner() {
        repository.handleChat(chat(11L, OTHER_ROOM, ME, "다른 기기에서 내가 보냄"), false);

        assertNull("내 메시지는 다른 방이어도 배너를 띄우지 않는다",
                repository.getBanner().getValue());
        assertEquals("방 목록은 갱신되어야 한다", OTHER_ROOM, rooms().get(0).getRoomId());
    }

    @Test
    public void messageAfterCloseRoom_isNotAdded() {
        repository.closeRoom(ACTIVE_ROOM);

        repository.handleChat(chat(12L, ACTIVE_ROOM, OTHER, "이미 나간 방"), false);

        assertEquals("방을 나간 뒤 도착한 메시지는 말풍선이 되면 안 된다", 0, bubbles().size());
    }

    @Test
    public void messageAfterLogout_isFullyDropped() {
        repository.setIdentity("", null);

        repository.handleChat(chat(20L, OTHER_ROOM, OTHER, "로그아웃 후 잔여 메시지"), false);

        assertNull("로그아웃 후 도착한 메시지는 배너를 띄우면 안 된다", repository.getBanner().getValue());
        assertEquals("말풍선도 추가되면 안 된다", 0, bubbles().size());
        assertNull("방 목록의 마지막 메시지도 갱신되면 안 된다", rooms().get(0).getLastChatMsg());
    }
}
