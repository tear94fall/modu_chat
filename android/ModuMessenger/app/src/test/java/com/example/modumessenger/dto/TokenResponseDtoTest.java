package com.example.modumessenger.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

public class TokenResponseDtoTest {

    @Test
    public void 스네이크_케이스_토큰_응답을_읽는다() {
        TokenResponseDto dto = new Gson().fromJson(
                "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", TokenResponseDto.class);
        assertEquals("at", dto.getAccessToken());
        assertEquals("rt", dto.getRefreshToken());
        assertEquals(3600L, dto.getExpiresIn());
        assertEquals("Bearer", dto.getTokenType());
    }

    @Test
    public void 리프레시가_없으면_null_이다() {
        TokenResponseDto dto = new Gson().fromJson("{\"access_token\":\"at\"}", TokenResponseDto.class);
        assertNull(dto.getRefreshToken());
    }
}
