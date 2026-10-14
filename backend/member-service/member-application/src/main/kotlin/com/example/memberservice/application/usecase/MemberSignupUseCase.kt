package com.example.memberservice.application.usecase

import com.example.memberservice.application.common.exception.CustomException
import com.example.memberservice.application.common.exception.ErrorCode
import com.example.memberservice.application.domain.entity.ProfileType
import com.example.memberservice.application.port.ProfileInfo
import com.example.memberservice.application.port.ProfilePort
import com.example.memberservice.application.port.StoragePort
import com.example.memberservice.application.service.MemberCommandService
import com.example.memberservice.application.usecase.command.GoogleAccountCommand
import com.example.memberservice.application.usecase.result.GoogleMemberResult
import com.example.memberservice.application.usecase.result.MemberResult
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

/**
 * 구글 계정으로 회원 찾기·가입(가입 = 첫 로그인). auth-service 가 구글 ID 토큰을 검증한 뒤 부른다.
 *
 * 1. 회원을 찾거나 만든다 — [MemberCommandService.findOrCreateGoogleMember]. 같은 계정의 동시 요청은 분산락이 줄 세우고,
 *    락 안의 일은 새 트랜잭션(master)에서 끝난다. 돌아왔을 때 회원은 이미 커밋돼 있다.
 * 2. 방금 만들었거나 되살린 회원이고 구글 사진이 있으면 storage 에 올리고 profile 에 이력을 남긴다 — 트랜잭션 밖.
 * 3. 그 결과를 회원에 반영한다 — 짧은 트랜잭션.
 *
 * 2·3 이 실패하면 1 을 되돌리고(새 회원은 지우고, 되살린 회원은 다시 탈퇴 상태로) 오류를 그대로 올린다 —
 * 예전에는 한 트랜잭션이라 함께 롤백됐고, 다음 로그인 때 처음부터 다시 했다. 그 동작을 지킨다.
 */
@Component
class MemberSignupUseCase(
    private val memberCommandService: MemberCommandService,
    private val storagePort: StoragePort,
    private val profilePort: ProfilePort,
) {

    private val log = LoggerFactory.getLogger(MemberSignupUseCase::class.java)

    fun findOrCreate(account: GoogleAccountCommand): MemberResult {
        val found = findOrCreateWithRetry(account)
        val picture = account.picture
        if (!found.fresh || picture.isNullOrBlank()) {
            return found.member
        }
        return try {
            attachGooglePicture(found.member, picture)
        } catch (e: Exception) {
            cancelQuietly(found)
            throw e
        }
    }

    /**
     * 동시에 같은 계정으로 가입 요청이 오면 하나만 INSERT 에 성공하고 나머지는 유일 제약에 걸린다.
     * 진 요청의 트랜잭션은 롤백 표시가 되어 그 안에서는 복구할 수 없으므로, 트랜잭션 밖에서
     * 한 번만 다시 부른다. 그때는 새 트랜잭션이라 상대가 커밋한 회원이 보이고 멱등 경로를 탄다.
     */
    private fun findOrCreateWithRetry(account: GoogleAccountCommand): GoogleMemberResult =
        try {
            memberCommandService.findOrCreateGoogleMember(account)
        } catch (e: DataIntegrityViolationException) {
            log.warn("동시 가입 경합 감지, 한 번 다시 시도한다: {}", e.message)
            memberCommandService.findOrCreateGoogleMember(account)
        }

    /** 구글 사진을 storage 에 올려 첫 프로필로 기록하고 회원에 반영한다. */
    private fun attachGooglePicture(member: MemberResult, picture: String): MemberResult {
        val memberId = member.id!!
        val uploadFile = storagePort.uploadFromUrl(picture)
        if (uploadFile.isNullOrEmpty()) {
            throw CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, picture)
        }

        val request = ProfileInfo(0L, memberId, ProfileType.PROFILE_IMAGE, uploadFile, "", "")
        val saved = profilePort.addProfile(request)
        if (saved == null || saved.value != uploadFile) {
            throw CustomException(ErrorCode.USER_PROFILE_IMAGE_UPLOAD_ERROR, uploadFile)
        }

        // 예전 코드와 같이 요청의 id(0)를 프로필 목록에 넣는다(저장된 이력의 id 가 아니다).
        return memberCommandService.applyProfileImage(memberId, saved.profileType, saved.value, request.id!!)
    }

    private fun cancelQuietly(found: GoogleMemberResult) {
        try {
            memberCommandService.cancelSignup(found.member.id!!, found.revived)
        } catch (e: Exception) {
            log.warn("구글 사진 단계 실패 뒤 가입 되돌리기도 실패 memberId={} message={}", found.member.id, e.message)
        }
    }
}
