package com.example.modumessenger.member.service;

import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.entity.Member;
import com.example.modumessenger.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;

    public MemberDto registerMember(MemberDto memberDto) {
        if(memberRepository.existsByEmail(memberDto.getEmail())) {
            throw new DuplicateKeyException(String.format(
                    "이미 존재하는 유저입니다 'auth: %s, email: %s'", memberDto.getAuth(), memberDto.getEmail()
            ));
        }

        Member member = new Member();
        member.setUserId(memberDto.getUserId());
        member.setUsername(memberDto.getUsername());
        member.setAuth(memberDto.getAuth());
        member.setEmail(memberDto.getEmail());
        member.setStatusMessage(memberDto.getStatusMessage());
        member.setProfileImage(memberDto.getProfileImage());
        member.setFriends(new ArrayList<>());

        Member save = memberRepository.save(member);

        System.out.println(save);

        return memberDto;
    }

    public MemberDto getUserIdByEmail(String email) {
        Member findMember = memberRepository.searchMemberByUserId(email).orElseGet(Member::new);
        return modelMapper.map(findMember, MemberDto.class);
    }

    public List<MemberDto> getFriendsList(String userId) {
        Member member = memberRepository.findByUserId(userId);
        List<Member> memberList = memberRepository.findAllFriends(member.getFriends());

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
        Member friend = memberRepository.findByEmail(memberDto.getEmail());

        memberRepository.findByUserId(userId).getFriends().add(friend.getId());
        memberRepository.save(member);

        return modelMapper.map(friend, MemberDto.class);
    }

    public List<MemberDto> findFriend(String email) {
        if(!memberRepository.existsByEmail(email)) {
            throw new DuplicateKeyException(String.format("존재하지 않는 유저의 이메일 입니다 'email: %s'", email));
        }

        List<Member> memberList = memberRepository.findFriendsByEmail(email);

        return memberList
                .stream()
                .map(u -> modelMapper.map(u, MemberDto.class))
                .collect(Collectors.toList());
    }
}
