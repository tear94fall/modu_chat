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
class ChatRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private EntityManager em;

    @BeforeEach
    void init() {
        ChatRoom chatRoomA = new ChatRoom("A", "Room A", "Image A", "Msg A", "", LocalDateTime.now().toString());
        ChatRoom chatRoomB = new ChatRoom("B", "Room B", "Image B", "Msg B", "", LocalDateTime.now().toString());

        chatRoomRepository.save(chatRoomA);
        chatRoomRepository.save(chatRoomB);

        Chat chatA = new Chat("Hello ABB!!", chatRoomA);
        Chat chatB = new Chat("Hello BBB!!", chatRoomB);
        Chat saveA = chatRepository.save(chatA);
        Chat saveB = chatRepository.save(chatB);

        chatRoomA.getChat().add(saveA);
        chatRoomB.getChat().add(saveB);

        chatRoomA.setLastChatId(saveA.getId().toString());
        chatRoomB.setLastChatId(saveB.getId().toString());

        chatRoomRepository.save(chatRoomA);
        chatRoomRepository.save(chatRoomB);

        em.flush();
        em.clear();
    }

    // N + 1
    @DisplayName("모든 채팅 가져오기 (N + 1)")
    @Test
    @Transactional
    public void findAllTest() {
        List<Chat> chats = chatRepository.findAll();

        chats.forEach(c -> {
            System.out.println(c);
            System.out.println(c.getChatRoom());
        });
    }

    @DisplayName("모든 채팅 가져오기 (페치 조인)")
    @Test
    @Transactional
    public void findAllTestQueryDsl() {
        List<Chat> chats = chatRepository.findAllQueryDsl();

        chats.forEach(c -> {
            System.out.println(c);
            System.out.println(c.getChatRoom());
        });
    }
}