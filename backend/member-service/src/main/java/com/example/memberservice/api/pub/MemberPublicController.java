package com.example.memberservice.api.pub;

import com.example.memberservice.member.dto.*;
import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.service.MemberFriendService;
import com.example.memberservice.member.service.MemberService;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.memberservice.member.dto.PageResponse;
import com.example.memberservice.member.repository.FriendSort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 안드로이드가 게이트웨이를 거쳐 부르는 회원 API. 가입은 auth-service 의 첫 로그인(내부 API)에서 일어난다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-public/member")
public class MemberPublicController {

    private final MemberService memberService;
    private final MemberFriendService memberFriendService;
    private final ProfileFeignClient profileFeignClient;
    private final ModelMapper modelMapper;

    @GetMapping("/{email}")
    public ResponseEntity<ResponseMemberDto> userId(@Valid @PathVariable("email") String email) {
        MemberDto memberDto = memberService.getMemberByEmail(email);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }

    @GetMapping("/member/{id}")
    public ResponseEntity<ResponseMemberDto> getMemberById(@Valid @PathVariable("id") Long id) {
        MemberDto memberDto = memberService.getMemberById(id);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }


    @PostMapping("/{userId}")
    public ResponseEntity<ResponseMemberDto> updateMemberProfileInfo(@Valid @PathVariable("userId") String userId, @RequestBody UpdateProfileDto updateProfileDto) {
        MemberDto memberDto = memberService.updateMemberProfile(userId, updateProfileDto);
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseEntity.ok().body(ResponseMemberDto.from(memberDto, profiles));
    }

    private static final int FRIENDS_DEFAULT_SIZE = 50;
    private static final int FRIENDS_MAX_SIZE = 100;

    /** 친구 목록. sort(기본 name,asc) 는 {@link FriendSort} 허용 목록만 받고, page/size 로 잘라 준다. size 는 최대 100. 항목의 friendName 이 내가 정한 이름. */
    @GetMapping("/{userId}/friends")
    public ResponseEntity<PageResponse<ResponseFriendDto>> friendsList(@Valid @PathVariable("userId") String userId,
                                                                       @RequestParam(value = "sort", defaultValue = "name,asc") String sort,
                                                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                                                       @RequestParam(value = "size", defaultValue = "" + FRIENDS_DEFAULT_SIZE) int size) {
        FriendSort friendSort = FriendSort.parse(sort)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 정렬입니다: " + sort));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), FRIENDS_MAX_SIZE));
        return ResponseEntity.ok(PageResponse.from(memberFriendService.getFriendsPage(userId, friendSort, pageable), f -> f));
    }

    /** friend userId → 내가 정한 이름. 앱이 채팅 화면·푸시 알림에서 치환할 때 쓴다. */
    @GetMapping("/{userId}/friends/names")
    public ResponseEntity<Map<String, String>> friendNames(@Valid @PathVariable("userId") String userId) {
        return ResponseEntity.ok(memberFriendService.getFriendNames(userId));
    }

    @PostMapping("/{userId}/friends")
    public ResponseEntity<ResponseFriendDto> addFriends(@Valid @PathVariable("userId") String userId, @RequestBody AddFriendDto addFriendDto) {
        return ResponseEntity.ok(memberFriendService.addFriend(userId, addFriendDto.getEmail()));
    }

    /** 내가 정한 친구 이름 변경. 공백만 있거나 255자를 넘으면 400, 친구가 아니면 404. */
    @PutMapping("/{userId}/friends/{friendMemberId}/name")
    public ResponseEntity<ResponseFriendDto> renameFriend(@Valid @PathVariable("userId") String userId,
                                                          @PathVariable("friendMemberId") Long friendMemberId,
                                                          @RequestBody RenameFriendRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty() || name.length() > MemberFriend.NAME_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "친구 이름은 1~255자여야 합니다.");
        }
        try {
            return ResponseEntity.ok(memberFriendService.renameFriend(userId, friendMemberId, name));
        } catch (CustomException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "친구가 아닙니다: " + friendMemberId, e);
        }
    }

    @GetMapping("/friends/{email}")
    public ResponseEntity<List<ResponseFriendDto>> findFriend(@Valid @PathVariable("email") String email) {
        List<ResponseFriendDto> result = memberService.findFriend(email).stream()
                .map(f -> modelMapper.map(f, ResponseFriendDto.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok().body(result);
    }
}
