package com.example.authservice.oauth.sso;

import com.example.authservice.api.pub.dto.SsoCodeResponse;
import com.example.authservice.oauth.config.OAuthProperties;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** 로그인된 앱(sso-issuer)이 다른 앱을 위해 1회용 코드를 만든다. 코드는 대상 클라이언트와 PKCE 챌린지에 묶인다. */
@Service
@RequiredArgsConstructor
public class SsoCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuthProperties props;
    private final SsoCodeStore store;

    public SsoCodeResponse issue(String issuerClientId, String sub, String targetClientId, String codeChallenge, String method) {
        OAuthProperties.Client issuer = props.client(issuerClientId);
        if (issuer == null || !issuer.isSsoIssuer()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이 앱은 다른 앱에 로그인을 넘겨줄 수 없습니다.");
        }
        OAuthProperties.Client target = props.client(targetClientId);
        if (target == null || !target.getGrants().contains("sso_code")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SSO 로그인을 받을 수 없는 앱입니다: " + targetClientId);
        }
        if (!"S256".equals(method) || !StringUtils.hasText(codeChallenge) || codeChallenge.length() < 43 || codeChallenge.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code_challenge(S256) 가 필요합니다.");
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        store.save(code, new SsoCode(sub, targetClientId, codeChallenge, method), props.getSsoCodeTtl());
        return new SsoCodeResponse(code, props.getSsoCodeTtl().toSeconds());
    }
}
