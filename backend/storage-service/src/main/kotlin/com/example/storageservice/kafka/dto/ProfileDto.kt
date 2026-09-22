package com.example.storageservice.kafka.dto

data class ProfileDto(
    var id: Long? = null,
    var memberId: Long? = null,
    var profileType: ProfileType? = null,
    var value: String? = null,
    var createdDate: String? = null,
    var updatedDate: String? = null,
)
