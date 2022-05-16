package com.example.modumessenger.chat.repository;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class ChatRoomRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private EntityManager em;

    @BeforeEach
    void init() {
        ChatRoom chatRoomA = new ChatRoom("A", "Room A", "Image A", "Msg A");
        ChatRoom chatRoomB = new ChatRoom("B", "Room B", "Image B", "Msg B");
        chatRoomRepository.save(chatRoomA);
        chatRoomRepository.save(chatRoomB);

        Chat chatA = new Chat("Hello ABB!!", chatRoomA);
        Chat chatB = new Chat("Hello BBB!!", chatRoomA);
        Chat chatC = new Chat("Hello CCC!!", chatRoomB);
        Chat chatD = new Chat("Hello DDD!!", chatRoomB);
        chatRepository.save(chatA);
        chatRepository.save(chatB);
        chatRepository.save(chatC);
        chatRepository.save(chatD);

        em.flush();
        em.clear();
    }

    // N + 1
    @DisplayName("모든 채팅방 가져오기 (N + 1")
    @Test
    @Transactional
    public void findAllTest() {
        List<ChatRoom> chatRoomList = chatRoomRepository.findAll();

        chatRoomList.forEach(r -> {
            System.out.println(r);
            System.out.println(r.getChat());
        });
    }

    @DisplayName("모든 채팅방 가져오기 (페치 조인)")
    @Test
    @Transactional
    public void findAllTestQueryDsl() {
        List<ChatRoom> chatRoomList = chatRoomRepository.findAllQueryDsl();

        chatRoomList.forEach(r -> {
            System.out.println(r);
            System.out.println(r.getChat());
        });
    }
}