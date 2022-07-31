package com.example.modumessenger.member.entity.friend;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class Hidden extends Friend {

    private FriendType hidden;
}
