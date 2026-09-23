package com.example.pointservice.common.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseTimeEntity {

    @Column(updatable = false)
    var createdDate: LocalDateTime? = null
        protected set

    var updatedDate: LocalDateTime? = null
        protected set

    @PrePersist
    fun prePersist() {
        createdDate = LocalDateTime.now()
        updatedDate = null
    }

    @PreUpdate
    fun preUpdate() {
        updatedDate = LocalDateTime.now()
    }
}
