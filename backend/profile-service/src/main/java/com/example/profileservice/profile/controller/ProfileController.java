package com.example.profileservice.profile.controller;

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
    public ResponseEntity<List<ProfileDto>> getProfiles(@PathVariable String memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfiles(memberId));
    }

    @GetMapping("/profile/{memberId}/{id}/{count}")
    public ResponseEntity<List<ProfileDto>> getProfilesOffset(@PathVariable String memberId, @PathVariable String id, @PathVariable String count) {
        return ResponseEntity.ok().body(profileService.getMemberProfileOffset(memberId, id, count));
    }

    @GetMapping("/profile/{memberId}/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable String memberId, @PathVariable String id) {
        return ResponseEntity.ok().body(profileService.getMemberProfile(memberId, id));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileDto> createProfile(@RequestBody ProfileDto profileDto) {
        return ResponseEntity.ok().body(profileService.registerProfile(profileDto));
    }

    @DeleteMapping("/profile/{memberId}/{id}")
    public ResponseEntity<Long> removeProfile(@PathVariable String memberId, @PathVariable String id) {
        return ResponseEntity.ok().body(profileService.deleteProfile(memberId, id));
    }
}
