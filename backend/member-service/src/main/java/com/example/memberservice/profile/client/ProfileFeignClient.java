package com.example.memberservice.profile.client;

import com.example.memberservice.profile.dto.ProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient("profile-service")
public interface ProfileFeignClient {

    @PostMapping("/profile")
    ResponseEntity<ProfileDto> addProfileRequest(@RequestBody ProfileDto profileDto);
}
