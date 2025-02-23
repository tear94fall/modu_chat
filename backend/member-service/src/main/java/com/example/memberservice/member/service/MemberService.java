package com.example.memberservice.member.service;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.global.lock.DistributedLock;
import com.example.memberservice.global.properties.GoogleOauthProperties;
import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.AddProfileDto;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.profile.dto.ProfileType;
import com.example.memberservice.storage.client.StorageFeignClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;
    private final GoogleOauthProperties googleOauthProperties;
    private final StorageFeignClient storageFeignClient;
    private final ProfileFeignClient profileFeignClient;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

        return new User(member.getEmail(), member.getUserId(),
                true, true, true, true, new ArrayList<>());
    }

    public MemberDto getUserById(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        return modelMapper.map(member, MemberDto.class);
    }

    public ResponseMemberDto createMember(GoogleLoginRequest googleLoginRequest) {
        MemberDto memberDto = registerMember(googleLoginRequest);
        MemberDto updateMemberDto = addProfileImage(memberDto);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();

        return ResponseMemberDto.from(updateMemberDto, profiles);
    }

    @DistributedLock(key = "#googleLoginRequest.getAuthType().concat('-').concat(#googleLoginRequest.getIdToken())")
    public MemberDto registerMember(GoogleLoginRequest googleLoginRequest) {
        Payload payload = GoogleIdTokenVerifier(googleLoginRequest.getIdToken());
        if (payload == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR, googleLoginRequest.getIdToken());
        }

        String email = payload.getEmail();
        if (memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.ALREADY_EXIST_USERID, email);
        }

        MemberDto memberDto = new MemberDto(payload);
        Member member = new Member(memberDto);

        Member saveMember = memberRepository.save(member);
        memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.CREATE_NEW_USER_FAIL, email));


        return MemberDto.createMemberDto(saveMember);
    }

    public MemberDto addProfileImage(MemberDto memberDto) {
        Member member = memberRepository.findById(memberDto.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, memberDto.getId()));

        String uploadFile = storageFeignClient.upload(member.getProfileImage()).getBody();
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, memberDto.getProfileImage());
        }

        ProfileDto profileDto = ProfileDto.from(0L, member.getId(), ProfileType.PROFILE_IMAGE, uploadFile, "", "");
        ProfileDto saveProfile = profileFeignClient.addProfileRequest(profileDto).getBody();

        if (saveProfile == null || !saveProfile.getValue().equals(uploadFile)) {
            throw new CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, uploadFile);
        }

        member.updateMemberInfo(saveProfile);
        member.addProfile(profileDto.getId());

        return MemberDto.createMemberDto(member);
    }

    public MemberDto getMemberById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id));
        return MemberDto.createMemberDto(member);
    }

    public MemberDto getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));
        return MemberDto.createMemberDto(member);
    }

    public MemberDto updateMemberProfile(String userId, UpdateProfileDto updateProfileDto) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        member.updateProfile(updateProfileDto);
        Member updateMember = memberRepository.save(member);

        return MemberDto.createMemberDto(member);
    }

    public MemberDto rollbackMemberProfile(Long id, ProfileDto profileDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id));

        List<ProfileDto> profileList = profileFeignClient.getMemberProfiles(member.getId()).getBody();
        if (profileList != null && profileList.isEmpty()) {
            ProfileDto profile = profileList.getLast();

            if (profileDto.getProfileType().equals(ProfileType.PROFILE_IMAGE) || profileDto.getProfileType().equals(ProfileType.PROFILE_WALLPAPER)) {
                UpdateProfileDto updateProfileDto = UpdateProfileDto.createUpdateProfileDto(new MemberDto(member), profile);
                return updateMemberProfile(member.getUserId(), updateProfileDto);
            }
        }

        return MemberDto.createMemberDto(member);
    }

    public List<MemberDto> getFriendsList(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        List<Member> friendList = memberRepository.findAllByIdIn(member.getFriends());

        return friendList
                .stream()
                .map(friend -> modelMapper.map(friend, MemberDto.class))
                .collect(Collectors.toList());
    }

    public MemberDto addFriends(String userId, String email) {
        if(!memberRepository.existsByEmail(email)) {
            throw new DuplicateKeyException(String.format(
                    "존재하지 않는 유저입니다 'email: %s'", email
            ));
        }

        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        Member friend = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

        member.addFriends(friend.getId());

        return modelMapper.map(friend, MemberDto.class);
    }

    public List<MemberDto> findFriend(String email) {
        if(!memberRepository.existsByEmail(email)) {
            return new ArrayList<>();
        }

        List<Member> memberList = memberRepository.findAllByEmail(email);

        return memberList
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    public Payload GoogleIdTokenVerifier(String token) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleOauthProperties.getClientId()))
                .build();

        try {
            GoogleIdToken googleIdToken = verifier.verify(token);
            return googleIdToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR, token);
        }
    }

    public List<MemberDto> findMembers(List<String> userIds) {
        List<Member> members = memberRepository.findAllByUserIdIn(userIds);

        return members
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    public List<MemberDto> findMembersById(List<Long> ids) {
        List<Member> members = memberRepository.findAllById(ids);
        return members
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    public List<MemberDto> inviteMembers(ChatRoomMemberDto chatRoomMemberDto) {
        List<Long> chatRoomMembersIds = chatRoomMemberDto.getChatRoomMembers()
                .stream()
                .map(MemberDto::getId)
                .collect(Collectors.toList());

        List<Member> members = memberRepository.findAllById(chatRoomMembersIds);

        Long chatRoomId = chatRoomMemberDto.getChatRoomId();

        List<Member> inviteMembers = members
                .stream()
                .peek(member -> member.addChatRoom(chatRoomId))
                .toList();

        return inviteMembers
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    public List<MemberDto> exitMembers(ChatRoomMemberDto chatRoomMemberDto) {
        List<Long> chatRoomMembersIds = chatRoomMemberDto.getChatRoomMembers()
                .stream()
                .map(MemberDto::getId)
                .collect(Collectors.toList());

        List<Member> members = memberRepository.findAllById(chatRoomMembersIds);

        Long chatRoomId = chatRoomMemberDto.getChatRoomId();

        List<Member> exitMembers = members
                .stream()
                .peek(member -> member.delChatRoom(chatRoomId))
                .toList();

        return exitMembers
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }

    public Long addMemberProfile(AddProfileDto addProfileDto) {
        Member member = memberRepository.findById(addProfileDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, addProfileDto.getMemberId()));

        member.addProfile(addProfileDto.getProfileId());

        return member.getProfiles().get(member.getProfiles().size()-1);
    }
}
