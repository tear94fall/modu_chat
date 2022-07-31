package com.example.modumessenger.member.entity.friend;

import com.example.modumessenger.member.entity.Member;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public abstract class Friend {

    @Id @GeneratedValue
    @Column(name = "friend_id")
    private Long id;

    @OneToMany
    private List<Member> friends = new ArrayList<>();
}
