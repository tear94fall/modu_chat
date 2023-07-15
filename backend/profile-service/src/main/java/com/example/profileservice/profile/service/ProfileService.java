package com.example.profileservice.profile.service;

import com.example.profileservice.profile.dto.ProfileDto;
import com.example.profileservice.profile.entity.Profile;
import com.example.profileservice.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    public List<ProfileDto> getMemberProfiles(String memberId) {
        List<Profile> profileList = profileRepository.findByMemberId(Long.valueOf(memberId));

        return profileList.stream()
                .map(ProfileDto::new)
                .collect(Collectors.toList());
    }

    public ProfileDto registerProfile(ProfileDto profileDto) {
        Profile profile = new Profile(profileDto);
        profileRepository.save(profile);

        return new ProfileDto(profile);
    }

    public Long deleteProfile(String memberId, String value) {
        return profileRepository.deleteByMemberProfile(Long.valueOf(memberId), value);
    }
}
