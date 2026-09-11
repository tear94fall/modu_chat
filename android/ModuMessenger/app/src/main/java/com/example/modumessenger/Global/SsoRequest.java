package com.example.modumessenger.Global;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** 다른 앱이 SSO 코드를 요청할 때 넘긴 값 검증. 허용 앱·클라이언트만, PKCE 챌린지는 S256 만 받는다. */
public final class SsoRequest {

    public static final String ACTION = "com.example.modumessenger.action.REQUEST_SSO_CODE";
    public static final List<String> ALLOWED_CALLERS = Arrays.asList("com.example.moducommerce");
    public static final List<String> ALLOWED_CLIENTS = Arrays.asList("modu-commerce");
    private static final Pattern CHALLENGE = Pattern.compile("^[A-Za-z0-9_-]{43,128}$");

    private final String clientId;
    private final String codeChallenge;
    private final String codeChallengeMethod;

    private SsoRequest(String clientId, String codeChallenge, String codeChallengeMethod) {
        this.clientId = clientId;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
    }

    /** 유효하지 않으면 null. */
    public static SsoRequest from(String clientId, String codeChallenge, String codeChallengeMethod) {
        if (clientId == null || !ALLOWED_CLIENTS.contains(clientId)) return null;
        if (codeChallenge == null || !CHALLENGE.matcher(codeChallenge).matches()) return null;
        if (!"S256".equals(codeChallengeMethod)) return null;
        return new SsoRequest(clientId, codeChallenge, codeChallengeMethod);
    }

    public static boolean isAllowedCaller(String callingPackage) {
        return callingPackage != null && ALLOWED_CALLERS.contains(callingPackage);
    }

    public String getClientId() { return clientId; }
    public String getCodeChallenge() { return codeChallenge; }
    public String getCodeChallengeMethod() { return codeChallengeMethod; }
}
