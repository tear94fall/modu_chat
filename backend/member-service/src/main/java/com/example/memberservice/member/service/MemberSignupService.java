package com.example.memberservice.member.service;

import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.example.memberservice.member.dto.ResponseMemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * MemberService.createMember 는 @ApiLock 이 열어 준 REQUIRES_NEW 트랜잭션 안에서 통째로 실행된다.
 * 동시에 같은 계정으로 가입 요청이 오면 하나만 INSERT 에 성공하고 나머지는 유일 제약에 걸려
 * DataIntegrityViolationException 이 난다 — 그 트랜잭션은 REPEATABLE READ 스냅샷 때문에 상대가
 * 커밋한 행을 볼 수 없고, 이미 롤백 표시가 되어 안에서는 복구가 불가능하다. 그래서 이 클래스는
 * @Transactional 을 붙이지 않고 트랜잭션 밖에서 한 번만 다시 부른다 — 그때는 새 트랜잭션이라
 * 상대가 커밋한 회원이 보이고 멱등 경로(findByEmail 히트)를 탄다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSignupService {

    private final MemberService memberService;

    /**
     * 동시에 같은 계정으로 가입 요청이 오면 하나만 INSERT 에 성공하고 나머지는 유일 제약에 걸린다.
     * 진 요청의 트랜잭션은 롤백 표시가 되어 그 안에서는 복구할 수 없으므로, 트랜잭션 밖에서
     * 한 번만 다시 부른다. 그때는 새 트랜잭션이라 상대가 커밋한 회원이 보이고 멱등 경로를 탄다.
     */
    public ResponseMemberDto signup(GoogleLoginRequest googleLoginRequest) {
        try {
            return memberService.createMember(googleLoginRequest);
        } catch (DataIntegrityViolationException e) {
            log.warn("동시 가입 경합 감지, 한 번 다시 시도한다: {}", e.getMessage());
            return memberService.createMember(googleLoginRequest);
        }
    }
}
