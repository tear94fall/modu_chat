package com.example.memberservice.profile.client;

import com.example.memberservice.profile.dto.ProfileDto;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@FeignClient("profile-service")
public interface ProfileFeignClient {

    @Retry(name = "memberProfileRetry", fallbackMethod = "retryGetMemberProfileFallback")
    @GetMapping("/profile/{memberId}")
    ResponseEntity<List<ProfileDto>> getMemberProfiles(@PathVariable("memberId") Long memberId);

    @PostMapping("/profile")
    ResponseEntity<ProfileDto> addProfileRequest(@RequestBody ProfileDto profileDto);

    default ResponseEntity<List<ProfileDto>> retryGetMemberProfileFallback(Exception e) {
        return ResponseEntity.ok(new ArrayList<>());
    }
}
