package com.example.memberservice.profile.client;

import com.example.memberservice.profile.dto.ProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient("profile-service")
public interface ProfileFeignClient {

    @GetMapping("/profile/{memberId}")
    ResponseEntity<List<ProfileDto>> getMemberProfiles(@PathVariable("memberId") Long memberId);

    @PostMapping("/profile")
    ResponseEntity<ProfileDto> addProfileRequest(@RequestBody ProfileDto profileDto);
}
