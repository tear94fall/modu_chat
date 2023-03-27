package com.example.memberservice.member.service;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.global.properties.GoogleOauthProperties;
import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.example.memberservice.member.dto.MemberDto;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.storage.client.StorageFeignClient;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
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

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

        return new User(member.getEmail(), member.getUserId(),
                true, true, true, true, new ArrayList<>());
    }

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;
    private final GoogleOauthProperties googleOauthProperties;
    private final StorageFeignClient storageFeignClient;

    public MemberDto getUserById(String userId) {
        Member member = memberRepository.findByUserId(userId);
        return modelMapper.map(member, MemberDto.class);
    }

    public MemberDto registerMember(GoogleLoginRequest googleLoginRequest) {
        Payload payload = GoogleIdTokenVerifier(googleLoginRequest.getIdToken());

        MemberDto memberDto = null;

        // need to add exception
        if (payload != null) {
            String email = payload.getEmail();

            if(memberRepository.existsByEmail(email)){
                Member findMember = memberRepository.findByEmail(email)
                        .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));

                return modelMapper.map(findMember, MemberDto.class);
            }

            memberDto = new MemberDto(payload);

            ResponseEntity<String> upload = storageFeignClient.upload(memberDto.getProfileImage());
            String filename = upload.getBody();
            memberDto.setProfileImage(filename);
        }

        Member save = memberRepository.save(new Member(memberDto));

        return new MemberDto(save);
    }

    public MemberDto getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, email));
        return new MemberDto(member);
    }

    public MemberDto getUserIdByEmail(String email) {
        Member findMember = memberRepository.searchMemberByUserId(email).orElseGet(Member::new);
        return modelMapper.map(findMember, MemberDto.class);
    }

    public MemberDto updateMember(String userId, MemberDto memberDto) {
        Member findMember = memberRepository.searchMemberByUserId(userId).orElseGet(Member::new);

        findMember.setUsername(memberDto.getUsername());
        findMember.checkProfileUpdate(memberDto);

        findMember.setStatusMessage(memberDto.getStatusMessage());
        findMember.setProfileImage(memberDto.getProfileImage());
        findMember.setWallpaperImage(memberDto.getWallpaperImage());

        Member updateMember = memberRepository.save(findMember);
        return modelMapper.map(updateMember, MemberDto.class);
    }

    public List<MemberDto> getFriendsList(String userId) {
        Member member = memberRepository.findByUserId(userId);
        List<Member> memberList = memberRepository.findAllFriends(member.getFriends())
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_FRIENDS_NOT_FOUND_ERROR, userId));

        return memberList
                .stream()
                .map(u -> modelMapper.map(u, MemberDto.class))
                .collect(Collectors.toList());
    }

    public MemberDto addFriends(String userId, MemberDto memberDto) {
        if(!memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicateKeyException(String.format(
                    "존재하지 않는 유저입니다 'auth: %s, email: %s'", memberDto.getAuth(), memberDto.getEmail()
            ));
        }

        Member member = memberRepository.findByUserId(userId);
        Member friend = memberRepository.findByEmail(memberDto.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_NOT_FOUND, memberDto.getEmail()));

        memberRepository.findByUserId(userId).getFriends().add(friend.getId());
        memberRepository.save(member);

        return modelMapper.map(friend, MemberDto.class);
    }

    public List<MemberDto> findFriend(String email) {
        if(!memberRepository.existsByEmail(email)) {
            throw new DuplicateKeyException(String.format("존재하지 않는 유저의 이메일 입니다 'email: %s'", email));
        }

        List<Member> memberList = memberRepository.findFriendsByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USEREMAIL_FRIENDS_NOT_FOUND_ERROR, email));

        return memberList
                .stream()
                .map(u -> modelMapper.map(u, MemberDto.class))
                .collect(Collectors.toList());
    }

    public Payload GoogleIdTokenVerifier(String id_token) {
        String tmp = googleOauthProperties.getClientId();
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleOauthProperties.getClientId()))
                .build();

        GoogleIdToken idToken = null;

        try {
            idToken = verifier.verify(id_token);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        return idToken.getPayload();
    }
}
