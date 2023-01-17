package com.example.modumessenger.dto;

public class GoogleLoginRequest {
    private String authType;
    private String idToken;

    public String getAuthType() { return authType; }
    public String getIdToken() { return idToken; }

    public void setAuthType(String authType) { this.authType = authType; }
    public void setIdToken(String idToken) { this.idToken = idToken; }

    public GoogleLoginRequest(String idToken) {
        setIdToken(idToken);
    }

    public GoogleLoginRequest(String idToken, String authType) {
        setIdToken(idToken);
        setAuthType(authType);
    }
}
