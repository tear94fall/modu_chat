package com.example.profileservice.profile.controller;

import com.example.profileservice.profile.dto.ProfileDto;
import com.example.profileservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/profile/{memberId}")
    public ResponseEntity<List<ProfileDto>> getProfiles(@PathVariable String memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfiles(memberId));
    }

    @PostMapping("/profile")
    public ResponseEntity<ProfileDto> createProfile(@RequestBody ProfileDto profileDto) {
        return ResponseEntity.ok().body(profileService.registerProfile(profileDto));
    }

    @DeleteMapping("/profile/{memberId}/{value}")
    public ResponseEntity<Long> removeProfile(@PathVariable String memberId, @PathVariable String value) {
        return ResponseEntity.ok().body(profileService.deleteProfile(memberId, value));
    }
}
