package com.example.profileservice.profile.controller;

import com.example.profileservice.profile.dto.CreateProfileDto;
import com.example.profileservice.profile.dto.ProfileDto;
import com.example.profileservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile/{memberId}")
    public ResponseEntity<List<ProfileDto>> getProfiles(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfiles(memberId));
    }

    @GetMapping("/profile/{memberId}/{id}/{count}")
    public ResponseEntity<List<ProfileDto>> getProfilesOffset(@PathVariable("memberId") String memberId, @PathVariable("id") String id, @PathVariable("count") String count) {
        return ResponseEntity.ok().body(profileService.getMemberProfileOffset(memberId, id, count));
    }

    @GetMapping("/profile/{memberId}/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable("memberId") String memberId, @PathVariable("id") String id) {
        return ResponseEntity.ok().body(profileService.getMemberProfile(memberId, id));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileDto> createProfile(@RequestBody CreateProfileDto createProfileDto) {
        return ResponseEntity.ok().body(profileService.registerProfile(createProfileDto));
    }

    @DeleteMapping("/profile/{memberId}/{id}")
    public ResponseEntity<Long> removeProfile(@PathVariable("memberId") String memberId, @PathVariable("id") String id) {
        return ResponseEntity.ok().body(profileService.deleteProfile(memberId, id));
    }
}
