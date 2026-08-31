package com.example.modumessenger.Repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.Global.socket.WebSocketManager;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.dto.ChatDto;
import com.example.modumessenger.dto.ChatRoomUnreadDto;
import com.example.modumessenger.entity.ChatRoom;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

public class ChatRepositoryTest {

    private static final String ACTIVE_ROOM = "room-active";
    private static final String OTHER_ROOM = "room-other";
    private static final String ME = "me";
    private static final String OTHER = "someone-else";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private ChatRepository repository;
    private WebSocketManager webSocketManager;

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

    @SuppressWarnings("unchecked")
    private static RetrofitChatRoomAPI chatRoomApiStub() {
        RetrofitChatRoomAPI api = mock(RetrofitChatRoomAPI.class);
        // Mockito 는 Call 반환 타입에 null 을 준다. 실제로 호출되는 엔드포인트는
        // 전부 막아두지 않으면 APIHelper 안에서 NPE 가 난다.
        when(api.RequestUpdateLastRead(anyString(), anyString())).thenReturn(mock(Call.class));
        when(api.RequestUnreadCounts(anyString())).thenReturn(mock(Call.class));
        when(api.RequestReadCursors(anyString())).thenReturn(mock(Call.class));
        when(api.RequestChatRooms(anyString())).thenReturn(mock(Call.class));
        return api;
    }

    @SuppressWarnings("unchecked")
    private static RetrofitChatAPI chatApiStub() {
        RetrofitChatAPI api = mock(RetrofitChatAPI.class);
        when(api.RequestChatListSize(anyString(), anyString())).thenReturn(mock(Call.class));
        return api;
    }

    /** 서버 응답 JSON 을 그대로 파싱한다. @SerializedName 매핑까지 함께 검증된다. */
    private static List<ChatRoomUnreadDto> unreadDtos(String json) {
        return new Gson().fromJson(json, new TypeToken<List<ChatRoomUnreadDto>>() {}.getType());
    }

    @Before
    public void setUp() {
        webSocketManager = mock(WebSocketManager.class);
        when(webSocketManager.send(anyString())).thenReturn(true);
        repository = new ChatRepository(
                webSocketManager,
                chatApiStub(),
                chatRoomApiStub());

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

    /** Transformations.map 은 관찰자가 붙어야 계산된다. */
    private int totalUnread() {
        int[] seen = {-1};
        Observer<Integer> observer = value -> seen[0] = value == null ? -1 : value;
        repository.getTotalUnreadCount().observeForever(observer);
        repository.getTotalUnreadCount().removeObserver(observer);
        return seen[0];
    }

    @Test
    public void totalUnread_sumsAcrossRooms() {
        repository.handleChat(chat(40L, OTHER_ROOM, OTHER, "하나"), false);
        repository.handleChat(chat(41L, OTHER_ROOM, OTHER, "둘"), false);

        assertEquals("탭 배지는 방 개수가 아니라 메시지 총합이다", 2, totalUnread());
    }

    @Test
    public void totalUnread_isZeroWhenNothingUnread() {
        assertEquals(0, totalUnread());
    }

    @Test
    public void totalUnread_dropsWhenRoomIsRead() {
        repository.handleChat(chat(42L, OTHER_ROOM, OTHER, "안 읽음"), false);
        assertEquals(1, totalUnread());

        repository.openRoom(OTHER_ROOM);

        assertEquals("방을 읽으면 합계도 줄어든다", 0, totalUnread());
    }

    @Test
    public void totalUnread_followsServerMerge() {
        repository.applyUnreadCounts(unreadDtos(
                "[{\"roomId\":\"" + OTHER_ROOM + "\",\"unreadChatCount\":7},"
                        + "{\"roomId\":\"" + ACTIVE_ROOM + "\",\"unreadChatCount\":2}]"));

        assertEquals(9, totalUnread());
    }

    private static Map<String, Long> cursors(Object... pairs) {
        Map<String, Long> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], ((Number) pairs[i + 1]).longValue());
        }
        return map;
    }

    @Test
    public void unreadCountFor_countsMembersBehindTheMessage() {
        Map<String, Long> cursors = cursors(ME, 10L, OTHER, 4L);

        assertEquals("커서가 뒤처진 상대 1명", 1, ChatRepository.unreadCountFor(7L, ME, cursors));
    }

    @Test
    public void unreadCountFor_excludesSender() {
        // 보낸 사람의 커서가 뒤처져 있어도 자기 메시지는 읽은 것으로 본다.
        Map<String, Long> cursors = cursors(ME, 0L, OTHER, 99L);

        assertEquals(0, ChatRepository.unreadCountFor(7L, ME, cursors));
    }

    @Test
    public void unreadCountFor_countsMissingCursorAsUnread() {
        Map<String, Long> cursors = new HashMap<>();
        cursors.put(ME, 10L);
        cursors.put(OTHER, null);

        assertEquals("커서가 없는 멤버는 한 번도 안 읽은 것이다", 1,
                ChatRepository.unreadCountFor(7L, ME, cursors));
    }

    @Test
    public void unreadCountFor_isZeroWhenEveryoneRead() {
        Map<String, Long> cursors = cursors(ME, 10L, OTHER, 10L);

        assertEquals(0, ChatRepository.unreadCountFor(7L, ME, cursors));
    }

    @Test
    public void unreadCountFor_countsBothOthersInGroupRoom() {
        Map<String, Long> cursors = cursors(ME, 10L, OTHER, 3L, "third-person", 3L);

        assertEquals(2, ChatRepository.unreadCountFor(7L, ME, cursors));
    }

    @Test
    public void applyReadCursors_recomputesEveryBubble() {
        repository.handleChat(chat(5L, ACTIVE_ROOM, ME, "첫번째"), false);
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "두번째"), false);

        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 7L));

        assertEquals("커서(7)보다 앞선 5번은 읽혔다", 0, bubbles().get(0).getUnreadCount());
        assertEquals("커서(7)보다 뒤인 9번은 아직 안 읽혔다", 1, bubbles().get(1).getUnreadCount());
    }

    @Test
    public void readReceived_dropsUnreadOnEveryBubble() {
        repository.handleChat(chat(5L, ACTIVE_ROOM, ME, "첫번째"), false);
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "두번째"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 0L));
        assertEquals(1, bubbles().get(1).getUnreadCount());

        repository.onReadReceived(ACTIVE_ROOM, OTHER, 9L);

        assertEquals("상대가 끝까지 읽으면 모든 말풍선이 0 이 된다",
                0, bubbles().get(0).getUnreadCount());
        assertEquals(0, bubbles().get(1).getUnreadCount());
    }

    @Test
    public void readReceived_forAnotherRoom_isIgnored() {
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "메시지"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 0L));

        repository.onReadReceived(OTHER_ROOM, OTHER, 9L);

        assertEquals("다른 방의 읽음은 활성 방에 영향이 없다", 1, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void newMessage_getsUnreadCountFromCurrentCursors() {
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 3L, OTHER, 3L));

        repository.handleChat(chat(10L, ACTIVE_ROOM, ME, "새 메시지"), false);

        assertEquals("새로 도착한 메시지도 현재 커서로 계산된다",
                1, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void applyReadCursors_neverMovesACursorBackwards() {
        // openRoom 이 GET(refreshReadCursors) 과 READ 프레임(sendReadReceipt)을 연달아 보낸다.
        // 소켓 브로드캐스트(최신)가 GET 응답(스냅샷)보다 먼저 도착하는 순서가 흔하다 —
        // 그 순서에서도 이미 반영된 최신 커서가 스냅샷 값으로 되돌아가면 안 된다.
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "메시지"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 4L));
        assertEquals(1, bubbles().get(0).getUnreadCount());

        repository.onReadReceived(ACTIVE_ROOM, OTHER, 9L);
        assertEquals(0, bubbles().get(0).getUnreadCount());

        // 뒤늦게 도착한 GET 응답이 OTHER 를 옛 스냅샷 값(4)으로 들고 있다.
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 4L));

        assertEquals("소켓으로 이미 반영된 최신 커서가 스냅샷 값으로 되돌아가면 안 된다",
                0, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void onReadReceived_beforeCursorsLoaded_doesNotSeedPartialMap() {
        // refreshReadCursors 응답이 아직 안 왔을 때(readCursors 가 비어 있을 때) READ 프레임이
        // 먼저 도착해도 새 키를 만들면 안 된다 — 그러면 그 한 명짜리 맵을 방 전체로 오인해
        // 다른 멤버 몫까지 이미 읽은 것으로 잘못 계산된다.
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "메시지"), false);

        repository.onReadReceived(ACTIVE_ROOM, OTHER, 5L);

        assertEquals("커서가 로드되기 전에는 계산하지 않는다", 0, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void onReadReceived_ignoresLowerCursorThanStored() {
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "메시지"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 9L));
        assertEquals(0, bubbles().get(0).getUnreadCount());

        // 늦게 도착한 옛 이벤트 — 이미 9까지 읽었는데 5로 되돌리려 한다.
        repository.onReadReceived(ACTIVE_ROOM, OTHER, 5L);

        assertEquals("낮은 값의 READ 이벤트는 커서를 되돌리면 안 된다", 0, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void switchingRooms_dropsPreviousRoomCursors() {
        repository.handleChat(chat(9L, ACTIVE_ROOM, ME, "메시지"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 9L, OTHER, 9L));
        assertEquals(0, bubbles().get(0).getUnreadCount());

        repository.openRoom(OTHER_ROOM);
        repository.closeRoom(OTHER_ROOM);
        repository.openRoom(ACTIVE_ROOM);

        repository.handleChat(chat(10L, ACTIVE_ROOM, ME, "새 메시지"), false);

        assertEquals("방을 나갔다 들어오면 이전 방의 커서가 남아있으면 안 된다",
                0, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void sendReadReceipt_sendsExpectedReadFrame() {
        // setUp 의 openRoom(ACTIVE_ROOM) 이 이미 READ 프레임을 한 번 보낸다.
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webSocketManager, atLeastOnce()).send(captor.capture());

        String sent = captor.getAllValues().get(captor.getAllValues().size() - 1);
        JsonObject frame = new Gson().fromJson(sent, JsonObject.class);

        assertEquals("READ", frame.get("type").getAsString());
        assertEquals(ACTIVE_ROOM, frame.get("roomId").getAsString());
        assertEquals(ME, frame.get("sender").getAsString());
    }

    private static int unreadOf(List<ChatRoom> rooms, String roomId) {
        for (ChatRoom room : rooms) {
            if (roomId.equals(room.getRoomId())) return room.getUnreadCount();
        }
        throw new AssertionError("방을 찾지 못했다: " + roomId);
    }

    @Test
    public void otherRoomMessage_incrementsUnread() {
        repository.handleChat(chat(30L, OTHER_ROOM, OTHER, "하나"), false);
        repository.handleChat(chat(31L, OTHER_ROOM, OTHER, "둘"), false);

        assertEquals("보고 있지 않은 방은 배지가 올라야 한다", 2, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void activeRoomMessage_doesNotIncrementUnread() {
        repository.handleChat(chat(32L, ACTIVE_ROOM, OTHER, "보고 있는 방"), false);

        assertEquals("보고 있는 방은 배지가 오르면 안 된다", 0, unreadOf(rooms(), ACTIVE_ROOM));
    }

    @Test
    public void myMessage_doesNotIncrementUnread() {
        repository.handleChat(chat(33L, OTHER_ROOM, ME, "다른 기기에서 내가 보냄"), false);

        assertEquals("내가 보낸 메시지는 배지 대상이 아니다", 0, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void gapRecovery_doesNotIncrementUnread() {
        repository.handleChat(chat(34L, OTHER_ROOM, OTHER, "복구분"), true);

        assertEquals("갭 복구분은 서버 값으로 채워지므로 중복 증가하면 안 된다",
                0, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void openRoom_clearsUnread() {
        repository.handleChat(chat(35L, OTHER_ROOM, OTHER, "안 읽음"), false);
        assertEquals(1, unreadOf(rooms(), OTHER_ROOM));

        repository.openRoom(OTHER_ROOM);

        assertEquals("방에 들어가면 배지가 즉시 사라진다", 0, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void closeRoom_clearsUnread() {
        repository.handleChat(chat(36L, OTHER_ROOM, OTHER, "안 읽음"), false);
        repository.openRoom(OTHER_ROOM);
        repository.handleChat(chat(37L, ACTIVE_ROOM, OTHER, "이전 방"), false);

        repository.closeRoom(OTHER_ROOM);

        assertEquals(0, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void applyUnreadCounts_mergesByRoomId() {
        repository.applyUnreadCounts(unreadDtos(
                "[{\"roomId\":\"" + OTHER_ROOM + "\",\"lastSendChatId\":9,"
                        + "\"lastReadChatId\":4,\"unreadChatCount\":5}]"));

        assertEquals("서버 값이 그대로 반영된다", 5, unreadOf(rooms(), OTHER_ROOM));
        assertEquals("응답에 없는 방은 0 이다", 0, unreadOf(rooms(), ACTIVE_ROOM));
    }

    @Test
    public void applyUnreadCounts_overwritesLocalIncrement() {
        repository.handleChat(chat(38L, OTHER_ROOM, OTHER, "로컬 +1"), false);
        assertEquals(1, unreadOf(rooms(), OTHER_ROOM));

        repository.applyUnreadCounts(unreadDtos(
                "[{\"roomId\":\"" + OTHER_ROOM + "\",\"unreadChatCount\":0}]"));

        assertEquals("서버가 진실이다", 0, unreadOf(rooms(), OTHER_ROOM));
    }

    @Test
    public void messageAfterLogout_isFullyDropped() {
        repository.setIdentity("", null);

        repository.handleChat(chat(20L, OTHER_ROOM, OTHER, "로그아웃 후 잔여 메시지"), false);

        assertNull("로그아웃 후 도착한 메시지는 배너를 띄우면 안 된다", repository.getBanner().getValue());
        assertEquals("말풍선도 추가되면 안 된다", 0, bubbles().size());
        assertNull("방 목록의 마지막 메시지도 갱신되면 안 된다", rooms().get(0).getLastChatMsg());
    }

    private static Map<Long, ChatBubble> bubblesById(Object... pairs) {
        Map<Long, ChatBubble> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            long id = ((Number) pairs[i]).longValue();
            map.put(id, new ChatBubble(chat(id, ACTIVE_ROOM, (String) pairs[i + 1], "메시지")));
        }
        return map;
    }

    @Test
    public void withImpliedCursors_treatsASentMessageAsRead() {
        // 상대가 8번을 보냈다면 그 방을 보고 있었다는 뜻이고, 8번까지는 읽은 것이다.
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 10L, OTHER, 0L), bubblesById(8L, OTHER), ME);

        assertEquals(Long.valueOf(8L), merged.get(OTHER));
    }

    @Test
    public void withImpliedCursors_usesTheHighestMessageTheySent() {
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 10L, OTHER, 0L), bubblesById(3L, OTHER, 8L, OTHER, 5L, OTHER), ME);

        assertEquals(Long.valueOf(8L), merged.get(OTHER));
    }

    @Test
    public void withImpliedCursors_neverLowersAStoredCursor() {
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 10L, OTHER, 20L), bubblesById(8L, OTHER), ME);

        assertEquals("추론은 커서를 올리기만 한다", Long.valueOf(20L), merged.get(OTHER));
    }

    @Test
    public void withImpliedCursors_fillsANullCursor() {
        Map<String, Long> stored = new HashMap<>();
        stored.put(ME, 10L);
        stored.put(OTHER, null);

        Map<String, Long> merged = ChatRepository.withImpliedCursors(stored, bubblesById(8L, OTHER), ME);

        assertEquals("커서가 없어도 보낸 메시지는 읽음의 증거다", Long.valueOf(8L), merged.get(OTHER));
    }

    @Test
    public void withImpliedCursors_ignoresSendersOutsideTheCursorMap() {
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 10L, OTHER, 0L), bubblesById(8L, "stranger"), ME);

        assertEquals("방 멤버가 아닌 발신자는 분모를 늘리면 안 된다", 2, merged.size());
        assertEquals(Long.valueOf(0L), merged.get(OTHER));
    }

    @Test
    public void laterMessageFromOther_clearsUnreadOnMyEarlierMessages() {
        // 1:1 방에서 내 메시지 다음에 상대 메시지가 왔다면, 상대는 내 것을 읽은 것이다.
        repository.handleChat(chat(5L, ACTIVE_ROOM, ME, "내 메시지"), false);
        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 5L, OTHER, 0L));
        assertEquals("아직 상대 커서가 0 이라 안 읽음 1", 1, bubbles().get(0).getUnreadCount());

        repository.handleChat(chat(9L, ACTIVE_ROOM, OTHER, "상대 답장"), false);

        assertEquals("상대가 답장을 보냈으므로 내 메시지는 읽힌 것이다",
                0, bubbles().get(0).getUnreadCount());
    }

    @Test
    public void nullCursorRoom_stillClearsUnreadFromTheirMessages() {
        // last_read_chat_id 가 NULL 로 남아 있는 기존 방. READ 프레임을 한 번도 못 받았어도
        // 상대가 보낸 메시지가 화면에 있으면 그것이 읽음의 증거다.
        Map<String, Long> nullCursors = new HashMap<>();
        nullCursors.put(ME, null);
        nullCursors.put(OTHER, null);

        repository.handleChat(chat(5L, ACTIVE_ROOM, ME, "내 메시지"), false);
        repository.handleChat(chat(9L, ACTIVE_ROOM, OTHER, "상대 답장"), false);
        repository.applyReadCursors(ACTIVE_ROOM, nullCursors);

        assertEquals(0, bubbles().get(0).getUnreadCount());
    }

    // ---------- 내 커서: 보고 있는 방은 끝까지 읽은 것이다 ----------

    @Test
    public void withImpliedCursors_treatsMyCursorAsTheWholeRoom() {
        // 내가 이 방을 보고 있다. openRoom 이 이미 서버에 READ 를 보냈으므로
        // 내 커서는 내가 보낸 마지막 메시지가 아니라 화면의 마지막 메시지까지다.
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 5L, OTHER, 0L), bubblesById(5L, ME, 9L, OTHER), ME);

        assertEquals(Long.valueOf(9L), merged.get(ME));
    }

    @Test
    public void withImpliedCursors_stillLimitsOthersToTheirOwnMessages() {
        Map<String, Long> merged = ChatRepository.withImpliedCursors(
                cursors(ME, 0L, OTHER, 0L), bubblesById(5L, OTHER, 9L, ME), ME);

        assertEquals("상대는 자기가 보낸 것까지만 읽은 것으로 본다",
                Long.valueOf(5L), merged.get(OTHER));
    }

    @Test
    public void othersMessageInMyOpenRoom_showsNoUnread() {
        // 실기기 검증에서 나온 결함: 방에 처음 들어간 순간 GET 스냅샷이 옛 커서를 들고 오면
        // 방금 읽은 상대 메시지에 숫자가 남았다.
        repository.handleChat(chat(5L, ACTIVE_ROOM, ME, "내 메시지"), false);
        repository.handleChat(chat(9L, ACTIVE_ROOM, OTHER, "상대 메시지"), false);

        repository.applyReadCursors(ACTIVE_ROOM, cursors(ME, 5L, OTHER, 9L));

        assertEquals("내가 보고 있는 방의 메시지에 안 읽음이 남으면 안 된다",
                0, bubbles().get(1).getUnreadCount());
    }

    // ---------- 끊긴 동안 실패한 READ 는 재연결 때 다시 보낸다 ----------

    @Test
    public void failedReadReceipt_isResentOnReconnect() {
        when(webSocketManager.send(anyString())).thenReturn(false);
        repository.openRoom(ACTIVE_ROOM);

        when(webSocketManager.send(anyString())).thenReturn(true);
        clearInvocations(webSocketManager);
        repository.onReconnected();

        assertEquals("재연결 후 READ 프레임이 다시 나가야 한다",
                1, readFramesFor(ACTIVE_ROOM));
    }

    @Test
    public void deliveredReadReceipt_isNotResentOnReconnect() {
        when(webSocketManager.send(anyString())).thenReturn(true);
        repository.openRoom(ACTIVE_ROOM);

        clearInvocations(webSocketManager);
        repository.onReconnected();

        assertEquals("이미 전달된 READ 를 재연결 때 또 보내면 안 된다",
                0, readFramesFor(ACTIVE_ROOM));
    }

    /** 마지막 clearInvocations 이후 나간, 그 방의 READ 프레임 수. */
    private int readFramesFor(String roomId) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(webSocketManager, atLeast(0)).send(captor.capture());

        int count = 0;
        for (String frame : captor.getAllValues()) {
            if (frame.contains("\"READ\"") && frame.contains(roomId)) count++;
        }
        return count;
    }
}
