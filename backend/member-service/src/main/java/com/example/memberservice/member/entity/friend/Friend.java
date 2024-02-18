package com.example.memberservice.member.entity.friend;

import com.example.memberservice.member.entity.Member;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public abstract class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_id")
    private Long id;

    @OneToMany
    private List<Member> friends = new ArrayList<>();
}
