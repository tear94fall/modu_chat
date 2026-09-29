package com.example.profileservice.application.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

/** profileType 은 옛 자바처럼 @Enumerated 없이(ordinal) 저장한다 — 기존 행과 호환. */
@Entity
class Profile(
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
) : BaseTimeEntity() {

    @Id
    @Column(name = "profile_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
