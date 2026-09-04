package com.example.chatservice.api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chatservice.api.admin.dto.AdminChatRoomSummaryDto;
import com.example.chatservice.chat.entity.ChatRoom;
import com.example.chatservice.chat.service.ChatRoomService;
import com.example.chatservice.chat.service.ChatService;
import java.util.List;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChatAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ChatRoomService chatRoomService;
    @MockitoBean ChatService chatService;

    @Test
    void withoutToken_is403() throws Exception {
        mockMvc.perform(get("/api-admin/chat/rooms")).andExpect(status().isForbidden());
    }

    @Test
    void rooms_returnsPage() throws Exception {
        ChatRoom room = new ChatRoom("r1", "room-name", "room.jpg", "hi", "1", "2024-01-01T00:00:00");
        AdminChatRoomSummaryDto dto = new AdminChatRoomSummaryDto(room);
        when(chatRoomService.searchChatRoomsForAdmin(any())).thenReturn(new PageImpl<>(List.of(dto)));
        mockMvc.perform(get("/api-admin/chat/rooms").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].roomName").value("room-name"))
                .andExpect(jsonPath("$.content[0].roomImage").value("room.jpg"));
    }

    @Test
    void chats_returnsPageNewestFirst() throws Exception {
        when(chatService.searchChatsForAdmin(eq("r1"), any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api-admin/chat/rooms/r1/chats").param("page", "0").param("size", "10").header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(chatService).searchChatsForAdmin(eq("r1"), captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdDate").getDirection());
    }

    @Test
    void list_sortsNewestFirst() throws Exception {
        when(chatRoomService.searchChatRoomsForAdmin(any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api-admin/chat/rooms").header("X-Internal-Token", "test-internal-token")).andExpect(status().isOk());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        org.mockito.Mockito.verify(chatRoomService).searchChatRoomsForAdmin(captor.capture());
        Sort sort = captor.getValue().getSort();
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("createdDate").getDirection());
        assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
    }
}
