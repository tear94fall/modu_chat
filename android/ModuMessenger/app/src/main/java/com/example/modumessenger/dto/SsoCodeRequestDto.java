package com.example.modumessenger.dto;

public class SsoCodeRequestDto {
    private String clientId;
    private String codeChallenge;
    private String codeChallengeMethod;

    public SsoCodeRequestDto(String clientId, String codeChallenge, String codeChallengeMethod) {
        this.clientId = clientId;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getClientId() { return clientId; }
    public String getCodeChallenge() { return codeChallenge; }
    public String getCodeChallengeMethod() { return codeChallengeMethod; }
}
