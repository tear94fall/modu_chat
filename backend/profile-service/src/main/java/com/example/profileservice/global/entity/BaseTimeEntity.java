package com.example.profileservice.global.entity;

import lombok.Getter;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

    @Column(updatable = false)
    private LocalDateTime createdDate;

    private LocalDateTime updatedDate;

    @PrePersist
    public void prePersist() {
        createdDate = LocalDateTime.now();
        updatedDate = null;
    }

    @PreUpdate
    public void preUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
