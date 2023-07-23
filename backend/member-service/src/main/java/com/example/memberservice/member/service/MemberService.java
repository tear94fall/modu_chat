package com.example.memberservice.member.service;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.global.properties.GoogleOauthProperties;
import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.dto.ChatRoomMemberDto;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.storage.client.StorageFeignClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
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

        String uploadFile = storageFeignClient.upload(memberDto.getProfileImage()).getBody();
        if (!member.getProfileImage().equals(uploadFile)) {
            throw new CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, member.getProfileImage());
        }

        ProfileDto profileDto = new ProfileDto();
        ProfileDto saveProfile = profileFeignClient.addProfileRequest(profileDto).getBody();

        member.addProfile(profileDto.getId());

        if (saveProfile == null || !saveProfile.getValue().equals(uploadFile)) {
            throw new CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, uploadFile);
        }

        Member saveMember = memberRepository.save(member);
        memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.CREATE_NEW_USER_FAIL, email));

        return new MemberDto(saveMember);
    }

    public MemberDto getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));
        return new MemberDto(member);
    }

    public MemberDto updateMember(String userId, MemberDto memberDto) {
        Member member = memberRepository.searchMemberByUserId(userId).orElseGet(Member::new);
        member.updateMember(memberDto);
        Member updateMember = memberRepository.save(member);
        return modelMapper.map(updateMember, MemberDto.class);
    }

    public List<MemberDto> getFriendsList(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        List<Member> friendList = memberRepository.findAllFriends(member.getFriends())
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_FRIENDS_NOT_FOUND_ERROR, userId));

        return friendList
                .stream()
                .map(friend -> modelMapper.map(friend, MemberDto.class))
                .collect(Collectors.toList());
    }

    public MemberDto addFriends(String userId, MemberDto memberDto) {
        if(!memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicateKeyException(String.format(
                    "존재하지 않는 유저입니다 'auth: %s, email: %s'", memberDto.getAuth(), memberDto.getEmail()
            ));
        }

        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        Member friend = memberRepository.findByEmail(member.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, memberDto.getEmail()));

        member.getFriends().add(friend.getId());

        return modelMapper.map(friend, MemberDto.class);
    }

    public List<MemberDto> findFriend(String email) {
        if(!memberRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_NOT_FOUND, email);
        }

        List<Member> memberList = memberRepository.findFriendsByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_EMAIL_FRIENDS_NOT_FOUND_ERROR, email));

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
        List<Member> members = memberRepository.findAllByUserIds(userIds).orElseGet(ArrayList::new);

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
                .collect(Collectors.toList());

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
                .collect(Collectors.toList());

        return exitMembers
                .stream()
                .map(MemberDto::new)
                .collect(Collectors.toList());
    }
}
