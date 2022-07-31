package com.example.modumessenger.member.entity.friend;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
public class Favorite extends Friend {

    private FriendType favorite;
}
