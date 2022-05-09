package com.example.modumessenger.chat.entity;

import com.example.modumessenger.common.domain.BaseTimeEntity;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @ElementCollection
    @CollectionTable(name = "chat_members", joinColumns = @JoinColumn(name = "user_ids"))
    @Column(name = "chat_members")
    private List<Long> userIds;
}
