package com.example.authservice.security.provider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.authservice.member.dto.MemberDto;
import com.example.authservice.member.dto.Role;
import com.example.authservice.member.service.MemberService;
import com.example.authservice.security.CustomAuthenticationToken;
import com.example.authservice.security.dto.RequestLoginDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CustomAuthenticationProviderTest {

    @Test
    void adminMember_cannotLoginThroughAppLogin() {
        MemberService memberService = mock(MemberService.class);
        MemberDto admin = new MemberDto();
        admin.setUserId("admin"); admin.setEmail("admin@modu.local"); admin.setRole(Role.ROLE_ADMIN);
        when(memberService.getMember("admin")).thenReturn(admin);

        CustomAuthenticationProvider provider = new CustomAuthenticationProvider();
        ReflectionTestUtils.setField(provider, "memberService", memberService);

        RequestLoginDto req = new RequestLoginDto();
        req.setUserId("admin"); req.setEmail("admin@modu.local");

        assertThrows(RuntimeException.class, () -> provider.authenticate(new CustomAuthenticationToken(req)));
    }
}
