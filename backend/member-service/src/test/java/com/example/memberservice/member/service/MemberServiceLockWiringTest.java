package com.example.memberservice.member.service;

import com.example.memberservice.global.lock.ApiLock;
import com.example.memberservice.global.lock.LockParam;
import com.example.memberservice.member.dto.GoogleLoginRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MemberService 에서 @ApiLock 이 createMember 로 옮겨졌고, registerMember 에는
 * 더 이상 붙어있지 않다는 것을 리플렉션으로 검증한다 (자기 호출 때문에 락이 걸리지 않던 문제의 회귀 방지).
 * Spring 컨텍스트 없이 애노테이션 메타데이터만 확인한다.
 */
class MemberServiceLockWiringTest {

    @Test
    void createMember_isAnnotatedWithApiLock_andItsParameterWithLockParam() throws NoSuchMethodException {
        Method createMember = MemberService.class.getMethod("createMember", GoogleLoginRequest.class);

        assertThat(createMember.getAnnotation(ApiLock.class)).isNotNull();

        Parameter[] parameters = createMember.getParameters();
        assertThat(parameters).hasSize(1);
        assertThat(parameters[0].getAnnotation(LockParam.class)).isNotNull();
    }

    @Test
    void createMember_hasNoMethodLevelTransactional() throws NoSuchMethodException {
        Method createMember = MemberService.class.getMethod("createMember", GoogleLoginRequest.class);

        // 여기에 전파 옵션(특히 NOT_SUPPORTED)을 다시 붙이면 AopForTransaction 이
        // REQUIRES_NEW 로 연 안쪽 트랜잭션을 다시 중단시킨다. 클래스 레벨 @Transactional(REQUIRED)
        // 이 그 트랜잭션에 합류하도록 메서드에는 아무것도 붙이지 않아야 한다.
        assertThat(createMember.getAnnotation(Transactional.class)).isNull();
    }

    @Test
    void registerMember_noLongerCarriesApiLock() throws NoSuchMethodException {
        // registerMember 는 이미 검증된 Payload 를 받아 신규 회원만 생성한다 (자기 호출 때문에
        // 락이 걸리지 않던 문제의 회귀 방지 + 멱등 처리를 위해 createMember 에서 분리됨).
        // 패키지 전용 메서드라 getDeclaredMethod 로 조회한다.
        Method registerMember = MemberService.class.getDeclaredMethod("registerMember", Payload.class);

        assertThat(registerMember.getAnnotation(ApiLock.class)).isNull();

        Parameter[] parameters = registerMember.getParameters();
        assertThat(parameters).hasSize(1);
        assertThat(parameters[0].getAnnotation(LockParam.class)).isNull();
    }
}
