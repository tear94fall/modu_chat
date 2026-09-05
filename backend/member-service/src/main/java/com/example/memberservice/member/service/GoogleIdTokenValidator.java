package com.example.memberservice.member.service;

import com.example.memberservice.global.exception.CustomException;
import com.example.memberservice.global.exception.ErrorCode;
import com.example.memberservice.global.properties.GoogleOauthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * 구글 id 토큰 검증만 담당한다. MemberService 에서 분리한 이유는 테스트에서
 * 실제 구글 네트워크 호출 없이 검증 결과를 목킹할 수 있어야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class GoogleIdTokenValidator {

    private final GoogleOauthProperties googleOauthProperties;

    public Payload verify(String idToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleOauthProperties.getClientId()))
                .build();

        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                return null;
            }
            return googleIdToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_GOOGLE_ID_TOKEN_ERROR, idToken);
        }
    }
}
