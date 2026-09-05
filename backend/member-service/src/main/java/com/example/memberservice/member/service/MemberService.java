package com.example.memberservice.member.service;

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto;
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto;
import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.global.lock.ApiLock;
import com.example.memberservice.global.lock.LockParam;
import com.example.memberservice.member.dto.*;
import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.profile.client.ProfileFeignClient;
import com.example.memberservice.profile.dto.AddProfileDto;
import com.example.memberservice.profile.dto.ProfileDto;
import com.example.memberservice.profile.dto.ProfileType;
import com.example.memberservice.storage.client.StorageFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final ModelMapper modelMapper;
    private final StorageFeignClient storageFeignClient;
    private final ProfileFeignClient profileFeignClient;
    private final GoogleIdTokenValidator googleIdTokenValidator;

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

    /**
     * ApiLockAop 이 @Order(HIGHEST_PRECEDENCE) 라 트랜잭션 어드바이스보다 먼저 실행되고,
     * 안쪽 트랜잭션은 AopForTransaction 의 REQUIRES_NEW 가 연다.
     * 전파 옵션을 덧붙이면 그 트랜잭션이 중단되므로 붙이지 않는다.
     *
     * 이미 가입한 계정이면 그대로 돌려준다. 재설치·기기 변경이 곧바로 로그인으로 이어져야 한다
     * (예전에는 이미 존재하는 이메일이면 예외를 던져서 500 으로 응답이 끊겼다).
     */
    @ApiLock
    public ResponseMemberDto createMember(@LockParam GoogleLoginRequest googleLoginRequest) {
        Payload payload = googleIdTokenValidator.verify(googleLoginRequest.getIdToken());
        if (payload == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR, googleLoginRequest.getIdToken());
        }

        Optional<Member> registered = memberRepository.findByEmail(payload.getEmail());
        if (registered.isPresent()) {
            return toResponse(MemberDto.createMemberDto(registered.get()));
        }

        MemberDto created = registerMember(payload);      // 신규 생성만
        return toResponse(addProfileImage(created));      // 프로필 이미지는 신규일 때만
    }

    private ResponseMemberDto toResponse(MemberDto memberDto) {
        List<ProfileDto> profiles = profileFeignClient.getMemberProfiles(memberDto.getId()).getBody();
        return ResponseMemberDto.from(memberDto, profiles);
    }

    MemberDto registerMember(Payload payload) {
        MemberDto memberDto = new MemberDto(payload);
        Member member = new Member(memberDto);

        // 동시에 들어온 두 요청이 모두 findByEmail 을 통과한 뒤 저장을 시도하면 DB 의 유일 제약
        // (uk_member_email/uk_member_user_id) 이 하나만 통과시키고 나머지는 DataIntegrityViolationException
        // 을 던진다. 같은 트랜잭션 안에서는 REPEATABLE READ 스냅샷 때문에 상대가 커밋한 행이 보이지
        // 않고 트랜잭션도 이미 롤백 표시가 되어 여기서 복구할 수 없다 — 그대로 흘려보내고
        // MemberSignupService 가 트랜잭션 밖에서 재시도한다.
        Member saveMember = memberRepository.save(member);
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

    /** 백오피스 검색. keyword 가 비면 전체. */
    public Page<AdminMemberSummaryDto> searchMembers(String keyword, Pageable pageable) {
        Page<Member> page = (keyword == null || keyword.isBlank())
                ? memberRepository.findAll(pageable)
                : memberRepository.findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCase(keyword, keyword, pageable);
        return page.map(AdminMemberSummaryDto::from);
    }

    /** 백오피스 상세: 회원 + 친구 수. */
    public AdminMemberDetailDto getMemberDetail(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, String.valueOf(id)));
        return toAdminMemberDetailDto(member);
    }

    /** 백오피스에서 로그인한 본인 정보. userId 는 게이트웨이가 넣어 준 값이다. */
    public AdminMemberDetailDto getMemberDetailByUserId(String userId) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));
        return toAdminMemberDetailDto(member);
    }

    /**
     * 백오피스에서 본인 정보를 고친다. null 인 필드는 기존 값을 유지한다 —
     * 엔티티의 updateProfile 은 네 필드를 통째로 덮어쓰기 때문이다.
     */
    public AdminMemberDetailDto updateMyProfile(String userId, UpdateProfileDto request) {
        Member member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId));

        UpdateProfileDto full = UpdateProfileDto.builder()
                .username(request.getUsername() == null ? member.getUsername() : request.getUsername())
                .statusMessage(request.getStatusMessage() == null ? member.getStatusMessage() : request.getStatusMessage())
                .profileImage(request.getProfileImage() == null ? member.getProfileImage() : request.getProfileImage())
                .wallpaperImage(request.getWallpaperImage() == null ? member.getWallpaperImage() : request.getWallpaperImage())
                .build();

        member.updateProfile(full);
        memberRepository.save(member);

        return toAdminMemberDetailDto(member);
    }

    private AdminMemberDetailDto toAdminMemberDetailDto(Member member) {
        List<Long> friendIds = member.getFriends() == null ? List.of() : List.copyOf(member.getFriends());

        // 친구가 없으면 조회 자체를 건너뛴다. findAllByIdIn 에 빈 목록을 넘기면 불필요한 IN () 질의가 나간다.
        List<AdminMemberSummaryDto> friends = friendIds.isEmpty()
                ? List.of()
                : memberRepository.findAllByIdIn(friendIds).stream()
                        .map(AdminMemberSummaryDto::from)
                        .collect(Collectors.toList());

        return new AdminMemberDetailDto(
                modelMapper.map(member, MemberDto.class), friendIds.size(), member.getCreatedDate(), friends);
    }
}
