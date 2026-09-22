package com.example.profileservice.api.pub

import com.example.profileservice.profile.dto.CreateProfileDto
import com.example.profileservice.profile.dto.ProfileDto
import com.example.profileservice.profile.service.ProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 안드로이드가 게이트웨이를 거쳐 부르는 프로필 API. */
@RestController
@RequestMapping("/api-public/profile")
class ProfilePublicController(private val profileService: ProfileService) {

    @GetMapping("/{memberId}/{id}")
    fun getProfile(@PathVariable("memberId") memberId: String, @PathVariable("id") id: String): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.getMemberProfile(memberId, id))

    @GetMapping("/{memberId}")
    fun getProfiles(@PathVariable("memberId") memberId: Long): ResponseEntity<List<ProfileDto>> =
        ResponseEntity.ok().body(profileService.getMemberProfiles(memberId))

    @GetMapping("/latest/{memberId}")
    fun getLatestProfile(@PathVariable("memberId") memberId: String): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.getMemberLatestProfile(memberId))

    @GetMapping("/{memberId}/{id}/{count}")
    fun getProfilesOffset(
        @PathVariable("memberId") memberId: String,
        @PathVariable("id") id: String,
        @PathVariable("count") count: String,
    ): ResponseEntity<List<ProfileDto>> = ResponseEntity.ok().body(profileService.getMemberProfileOffset(memberId, id, count))

    @GetMapping("/total/count/{memberId}")
    fun getTotalProfileCount(@PathVariable("memberId") memberId: String): ResponseEntity<Long> =
        ResponseEntity.ok().body(profileService.getMemberProfileTotalCount(memberId))

    @PostMapping
    fun createProfile(@RequestBody createProfileDto: CreateProfileDto): ResponseEntity<ProfileDto> =
        ResponseEntity.ok().body(profileService.registerProfile(createProfileDto))

    @DeleteMapping("/{memberId}/{id}")
    fun removeProfile(@PathVariable("memberId") memberId: String, @PathVariable("id") id: String): ResponseEntity<Long> =
        ResponseEntity.ok().body(profileService.deleteProfile(memberId, id))
}
