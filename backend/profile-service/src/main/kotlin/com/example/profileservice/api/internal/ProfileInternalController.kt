package com.example.profileservice.api.internal

import com.example.profileservice.profile.dto.CreateProfileDto
import com.example.profileservice.profile.dto.ProfileDto
import com.example.profileservice.profile.service.ProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequestMapping("/api-internal/profile")
class ProfileInternalController(private val profileService: ProfileService) {

    @GetMapping("/{memberId}")
    fun getProfiles(@PathVariable("memberId") memberId: Long): ResponseEntity<List<ProfileDto>> =
        ResponseEntity.ok().body(profileService.getMemberProfiles(memberId))

    @PostMapping
    fun createProfile(@RequestBody createProfileDto: CreateProfileDto): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.registerProfile(createProfileDto))
}
