package com.example.authservice.oauth.grant.sso

import com.example.authservice.oauth.config.AuthorizationServerConfig
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken

class SsoCodeGrantToken(
    clientPrincipal: Authentication,
    val code: String,
    val codeVerifier: String,
    val scopes: Set<String>,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(AuthorizationServerConfig.SSO_CODE, clientPrincipal, additionalParameters)
