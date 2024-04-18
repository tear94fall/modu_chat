package com.example.memberservice.member.entity.friend;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Entity;

@Entity
@Getter
@Setter
public class Block extends Friend {

    private FriendType block;
}

