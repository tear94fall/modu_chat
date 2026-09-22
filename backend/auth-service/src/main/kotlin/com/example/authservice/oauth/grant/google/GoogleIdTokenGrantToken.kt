package com.example.authservice.oauth.grant.google

import com.example.authservice.oauth.config.AuthorizationServerConfig
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken

class GoogleIdTokenGrantToken(
    clientPrincipal: Authentication,
    val idToken: String,
    val scopes: Set<String>,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(AuthorizationServerConfig.GOOGLE_ID_TOKEN, clientPrincipal, additionalParameters)
