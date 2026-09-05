package com.example.profileservice.api.internal;

import com.example.profileservice.profile.dto.CreateProfileDto;
import com.example.profileservice.profile.dto.ProfileDto;
import com.example.profileservice.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** member-service 가 Feign 으로 부르는 API. InternalApiFilter 가 보호한다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-internal/profile")
public class ProfileInternalController {

    private final ProfileService profileService;

    @GetMapping("/{memberId}")
    public ResponseEntity<List<ProfileDto>> getProfiles(@PathVariable("memberId") Long memberId) {
        return ResponseEntity.ok().body(profileService.getMemberProfiles(memberId));
    }

    @PostMapping
    public ResponseEntity<ProfileDto> createProfile(@RequestBody CreateProfileDto createProfileDto) {
        return ResponseEntity.ok().body(profileService.registerProfile(createProfileDto));
    }
}
