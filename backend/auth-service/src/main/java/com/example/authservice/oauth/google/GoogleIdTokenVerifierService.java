package com.example.authservice.oauth.google;

import com.example.authservice.oauth.config.OAuthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Service;

/**
 * 구글 ID 토큰 검증. 서명·만료·audience(우리 앱 클라이언트 ID)를 확인한다.
 * 테스트에서 네트워크 없이 목킹할 수 있게 별도 빈으로 둔다.
 */
@Service
@RequiredArgsConstructor
public class GoogleIdTokenVerifierService {

    private final OAuthProperties props;

    public GoogleAccount verify(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(props.getGoogle().getAudiences())
                    .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw invalid("구글 ID 토큰이 유효하지 않습니다.");
            }
            GoogleIdToken.Payload p = token.getPayload();
            return new GoogleAccount(p.getSubject(), p.getEmail(),
                    p.get("name") == null ? "" : String.valueOf(p.get("name")),
                    p.get("picture") == null ? "" : String.valueOf(p.get("picture")));
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw invalid("구글 ID 토큰을 검증하지 못했습니다.");
        }
    }

    private static OAuth2AuthenticationException invalid(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, message, null));
    }
}
