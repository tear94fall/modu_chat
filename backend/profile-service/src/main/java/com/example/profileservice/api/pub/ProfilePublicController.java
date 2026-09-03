package com.example.profileservice.api.pub;

import com.example.profileservice.profile.dto.CreateProfileDto;
import com.example.profileservice.profile.dto.ProfileDto;
import com.example.profileservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 안드로이드가 게이트웨이를 거쳐 부르는 프로필 API. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/profile")
public class ProfilePublicController {

    private final ProfileService profileService;

    @GetMapping("/{memberId}/{id}")
    public ResponseEntity<ProfileDto> getProfile(@PathVariable("memberId") String memberId, @PathVariable("id") String id) {
        return ResponseEntity.ok().body(profileService.getMemberProfile(memberId, id));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<List<ProfileDto>> getProfiles(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfiles(memberId));
    }

    @GetMapping("/latest/{memberId}")
    public ResponseEntity<ProfileDto> getLatestProfile(@PathVariable("memberId") String memberId) {
        return ResponseEntity.ok().body(profileService.getMemberLatestProfile(memberId));
    }

    @GetMapping("/{memberId}/{id}/{count}")
    public ResponseEntity<List<ProfileDto>> getProfilesOffset(@PathVariable("memberId") String memberId, @PathVariable("id") String id, @PathVariable("count") String count) {
        return ResponseEntity.ok().body(profileService.getMemberProfileOffset(memberId, id, count));
    }

    @GetMapping("/total/count/{memberId}")
    public ResponseEntity<Long> getTotalProfileCount(@PathVariable("memberId") String memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfileTotalCount(memberId));
    }

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@RequestBody CreateProfileDto createProfileDto) {
        return ResponseEntity.ok().body(profileService.registerProfile(createProfileDto));
    }

    @DeleteMapping("/{memberId}/{id}")
    public ResponseEntity<Long> removeProfile(@PathVariable("memberId") String memberId, @PathVariable("id") String id) {
        return ResponseEntity.ok().body(profileService.deleteProfile(memberId, id));
    }
}
