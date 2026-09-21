package com.example.authservice.oauth.grant.admin

import com.example.authservice.oauth.config.AuthorizationServerConfig
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken

class AdminPasswordGrantToken(
    clientPrincipal: Authentication,
    val email: String,
    val password: String,
    val scopes: Set<String>,
    additionalParameters: Map<String, Any>,
) : OAuth2AuthorizationGrantAuthenticationToken(AuthorizationServerConfig.ADMIN_PASSWORD, clientPrincipal, additionalParameters)
