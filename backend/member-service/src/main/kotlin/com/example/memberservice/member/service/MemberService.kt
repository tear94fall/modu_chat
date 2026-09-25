package com.example.memberservice.member.service

import com.example.memberservice.api.admin.dto.AdminMemberDetailDto
import com.example.memberservice.api.admin.dto.AdminMemberSummaryDto
import com.example.memberservice.chat.client.ChatFeignClient
import com.example.memberservice.global.exception.CustomException
import com.example.memberservice.global.exception.ErrorCode
import com.example.memberservice.global.lock.ApiLock
import com.example.memberservice.global.lock.LockParam
import com.example.memberservice.member.dto.ChatRoomMemberDto
import com.example.memberservice.member.dto.GoogleAccountDto
import com.example.memberservice.member.dto.MemberDto
import com.example.memberservice.member.dto.UpdateProfileDto
import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.MemberFriendRepository
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.member.repository.MemberSort
import com.example.memberservice.notice.client.PushFeignClient
import com.example.memberservice.profile.client.ProfileFeignClient
import com.example.memberservice.profile.dto.AddProfileDto
import com.example.memberservice.profile.dto.ProfileDto
import com.example.memberservice.profile.dto.ProfileType
import com.example.memberservice.staff.StaffPermission
import com.example.memberservice.staff.StaffRepository
import com.example.memberservice.staff.sortedPermissions
import com.example.memberservice.storage.client.StorageFeignClient
import org.modelmapper.ModelMapper
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberService(
    private val memberRepository: MemberRepository,
    private val modelMapper: ModelMapper,
    private val storageFeignClient: StorageFeignClient,
    private val profileFeignClient: ProfileFeignClient,
    private val memberFriendService: MemberFriendService,
    private val memberFriendRepository: MemberFriendRepository,
    private val chatFeignClient: ChatFeignClient,
    private val pushFeignClient: PushFeignClient,
    private val staffRepository: StaffRepository,
) : UserDetailsService {

    private val log = LoggerFactory.getLogger(MemberService::class.java)

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(email: String): UserDetails {
        val member = memberRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND, email) }

        return User(member.email, member.userId, true, true, true, true, ArrayList())
    }

    fun getUserById(userId: String): MemberDto {
        val member = memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }

        return modelMapper.map(member, MemberDto::class.java)
    }

    /**
     * auth-service 가 구글 ID 토큰을 검증한 뒤 부른다. 이메일로 찾고 없으면 만든다(가입 = 첫 로그인).
     * 새 회원이고 구글 프로필 사진이 있으면 storage 에 올려 첫 프로필로 기록한다.
     */
    @ApiLock
    fun findOrCreateGoogleMember(@LockParam account: GoogleAccountDto): MemberDto {
        val registered = memberRepository.findByEmail(account.email!!)
        if (registered.isPresent) {
            return MemberDto.createMemberDto(registered.get())
        }

        // 탈퇴했던 계정(같은 구글 sub)이면 새 행을 만들지 않고 그 행을 되살린다. uk_member_user_id 때문에도 그래야 한다.
        val withdrawn = memberRepository.findByUserId(account.sub!!).filter { it.isWithdrawn }
        if (withdrawn.isPresent) {
            val member = withdrawn.get()
            member.reactivate(account.email!!, account.name)
            val revived = MemberDto.createMemberDto(member)
            if (!account.picture.isNullOrBlank()) {
                return addProfileImage(revived)
            }
            return revived
        }

        val created = registerMember(account)
        if (!account.picture.isNullOrBlank()) {
            return addProfileImage(created)
        }
        return created
    }

    /**
     * 회원 탈퇴(본인만, 컨트롤러가 확인한다). 이미 탈퇴한 회원이면 아무것도 하지 않는다.
     * 다른 서비스 정리(방 나가기·푸시 토큰)가 실패하면 예외가 그대로 올라가 탈퇴도 되지 않는다 — 앱이 다시 시도한다.
     * 사진 파일 삭제만은 실패해도 탈퇴를 막지 않는다.
     */
    fun withdraw(userId: String) {
        val member = memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, userId) }
        if (member.isWithdrawn) {
            return
        }

        chatFeignClient.exitAllChatRooms(member.id!!)
        pushFeignClient.deleteToken(userId)
        memberFriendRepository.deleteAllByMember_IdOrFriend_Id(member.id!!, member.id!!)
        deleteFileQuietly(member.profileImage)
        deleteFileQuietly(member.wallpaperImage)

        member.withdraw()
    }

    private fun deleteFileQuietly(file: String?) {
        if (file.isNullOrBlank()) return
        try {
            storageFeignClient.delete(file)
        } catch (e: RuntimeException) {
            log.warn("탈퇴 회원의 파일 삭제 실패, 계속 진행 file={} message={}", file, e.message)
        }
    }

    /** 검증된 계정으로 신규 회원만 만든다. 락과 멱등 처리는 findOrCreateGoogleMember 가 맡는다. */
    fun registerMember(account: GoogleAccountDto): MemberDto {
        val memberDto = MemberDto(account)
        val member = Member(memberDto)

        // 동시에 들어온 두 요청이 모두 findByEmail 을 통과한 뒤 저장을 시도하면 DB 의 유일 제약
        // (uk_member_email/uk_member_user_id) 이 하나만 통과시키고 나머지는 DataIntegrityViolationException
        // 을 던진다. 같은 트랜잭션 안에서는 REPEATABLE READ 스냅샷 때문에 상대가 커밋한 행이 보이지
        // 않고 트랜잭션도 이미 롤백 표시가 되어 여기서 복구할 수 없다 — 그대로 흘려보내고
        // MemberSignupService 가 트랜잭션 밖에서 재시도한다.
        val saveMember = memberRepository.save(member)
        return MemberDto.createMemberDto(saveMember)
    }

    fun addProfileImage(memberDto: MemberDto): MemberDto {
        val member = memberRepository.findById(memberDto.id!!)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, memberDto.id) }

        val uploadFile = storageFeignClient.upload(member.profileImage!!).body
        if (uploadFile.isNullOrEmpty()) {
            throw CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, memberDto.profileImage ?: "")
        }

        val profileDto = ProfileDto.from(0L, member.id, ProfileType.PROFILE_IMAGE, uploadFile, "", "")
        val saveProfile = profileFeignClient.addProfileRequest(profileDto).body

        if (saveProfile == null || saveProfile.value != uploadFile) {
            throw CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, uploadFile)
        }

        member.updateMemberInfo(saveProfile)
        member.addProfile(profileDto.id!!)

        return MemberDto.createMemberDto(member)
    }

    fun getMemberById(id: Long): MemberDto {
        val member = memberRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id) }
        return MemberDto.createMemberDto(member)
    }

    fun getMemberByEmail(email: String): MemberDto {
        val member = memberRepository.findByEmail(email)
            .orElseThrow { CustomException(ErrorCode.EMAIL_NOT_FOUND, email) }
        return MemberDto.createMemberDto(member)
    }

    fun updateMemberProfile(userId: String, updateProfileDto: UpdateProfileDto): MemberDto {
        val member = memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }

        member.updateProfile(updateProfileDto)
        memberRepository.save(member)

        return MemberDto.createMemberDto(member)
    }

    fun rollbackMemberProfile(id: Long, profileDto: ProfileDto): MemberDto {
        val member = memberRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id) }

        val profileList = profileFeignClient.getMemberProfiles(member.id!!).body
        if (profileList != null && profileList.isEmpty()) {
            val profile = profileList.last()

            if (profileDto.profileType == ProfileType.PROFILE_IMAGE || profileDto.profileType == ProfileType.PROFILE_WALLPAPER) {
                val updateProfileDto = UpdateProfileDto.createUpdateProfileDto(MemberDto(member), profile)
                return updateMemberProfile(member.userId, updateProfileDto)
            }
        }

        return MemberDto.createMemberDto(member)
    }

    fun findFriend(email: String): List<MemberDto> {
        if (!memberRepository.existsByEmail(email)) {
            return ArrayList()
        }

        val memberList = memberRepository.findAllByEmail(email)

        return memberList.map { MemberDto(it) }
    }

    fun findMembers(userIds: List<String>): List<MemberDto> {
        val members = memberRepository.findAllByUserIdIn(userIds)

        return members.map { MemberDto(it) }
    }

    fun findMembersById(ids: List<Long>): List<MemberDto> {
        val members = memberRepository.findAllById(ids)
        return members.map { MemberDto(it) }
    }

    fun inviteMembers(chatRoomMemberDto: ChatRoomMemberDto): List<MemberDto> {
        val chatRoomMembersIds = chatRoomMemberDto.chatRoomMembers.map { it.id }

        val members = memberRepository.findAllById(chatRoomMembersIds)

        val chatRoomId = chatRoomMemberDto.chatRoomId!!

        val inviteMembers = members.onEach { member -> member.addChatRoom(chatRoomId) }

        return inviteMembers.map { MemberDto(it) }
    }

    fun exitMembers(chatRoomMemberDto: ChatRoomMemberDto): List<MemberDto> {
        val chatRoomMembersIds = chatRoomMemberDto.chatRoomMembers.map { it.id }

        val members = memberRepository.findAllById(chatRoomMembersIds)

        val chatRoomId = chatRoomMemberDto.chatRoomId!!

        val exitMembers = members.onEach { member -> member.delChatRoom(chatRoomId) }

        return exitMembers.map { MemberDto(it) }
    }

    fun addMemberProfile(addProfileDto: AddProfileDto): Long {
        val member = memberRepository.findById(addProfileDto.memberId!!)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, addProfileDto.memberId) }

        member.addProfile(addProfileDto.profileId!!)

        return member.profiles!!.last()
    }

    /**
     * 백오피스 검색. keyword 가 비면 전체.
     * 정렬은 [MemberSort] 로 정한다(기본 이름 가나다순, 한글 이름 먼저). Pageable 의 sort 는 쓰지 않는다 —
     * "한글 먼저" 는 컬럼 하나로 표현할 수 없어 QueryDSL CASE 로 만들어야 한다.
     */
    @Transactional(readOnly = true)
    fun searchMembers(keyword: String?, sort: MemberSort?, pageable: Pageable): Page<AdminMemberSummaryDto> {
        val page = memberRepository.searchForAdmin(keyword, sort ?: MemberSort.DEFAULT, pageable)
        val staff = staffPermissionsOf(page.content.mapNotNull { it.id })
        return page.map { AdminMemberSummaryDto.from(it).withStaff(staff[it.id].orEmpty()) }
    }

    /** memberId → 직원 권한(SUPER, ADMIN, SYSTEM, INTERNAL 순). 직원이 아닌 회원은 맵에 없다. */
    private fun staffPermissionsOf(memberIds: Collection<Long>): Map<Long, List<StaffPermission>> =
        if (memberIds.isEmpty()) {
            emptyMap()
        } else {
            staffRepository.findAllByMemberIdIn(memberIds).associate { it.memberId to it.sortedPermissions() }
        }

    /** 백오피스 상세: 회원 + 친구 수. */
    fun getMemberDetail(id: Long): AdminMemberDetailDto {
        val member = memberRepository.findById(id)
            .orElseThrow { CustomException(ErrorCode.MEMBER_ID_NOT_FOUND_ERROR, id.toString()) }
        return toAdminMemberDetailDto(member)
    }

    /** 백오피스에서 로그인한 본인 정보. userId 는 게이트웨이가 넣어 준 값이다. */
    fun getMemberDetailByUserId(userId: String): AdminMemberDetailDto {
        val member = memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }
        return toAdminMemberDetailDto(member)
    }

    /**
     * 백오피스에서 본인 정보를 고친다. null 인 필드는 기존 값을 유지한다 —
     * 엔티티의 updateProfile 은 네 필드를 통째로 덮어쓰기 때문이다.
     */
    fun updateMyProfile(userId: String, request: UpdateProfileDto): AdminMemberDetailDto {
        val member = memberRepository.findByUserId(userId)
            .orElseThrow { CustomException(ErrorCode.USERID_NOT_FOUND_ERROR, userId) }

        val full = UpdateProfileDto(
            username = request.username ?: member.username,
            statusMessage = request.statusMessage ?: member.statusMessage,
            profileImage = request.profileImage ?: member.profileImage,
            wallpaperImage = request.wallpaperImage ?: member.wallpaperImage,
        )

        member.updateProfile(full)
        memberRepository.save(member)

        return toAdminMemberDetailDto(member)
    }

    private fun toAdminMemberDetailDto(member: Member): AdminMemberDetailDto {
        val friends = memberFriendService.listForAdmin(member.id!!)
        val staff = staffPermissionsOf(friends.mapNotNull { it.id } + member.id!!)
        return AdminMemberDetailDto(
            modelMapper.map(member, MemberDto::class.java), friends.size, member.createdDate,
            friends.map { it.withStaff(staff[it.id].orEmpty()) }, staff[member.id].orEmpty(),
        )
    }
}
