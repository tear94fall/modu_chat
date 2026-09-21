package com.example.authservice.oauth.grant

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

/** client_id 가 등록된 공개 클라이언트인지 확인한다. */
class PublicClientAuthenticationProvider(private val clients: RegisteredClientRepository) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? {
        val token = authentication as OAuth2ClientAuthenticationToken
        if (ClientAuthenticationMethod.NONE != token.clientAuthenticationMethod) {
            return null
        }
        val rc = clients.findByClientId(token.principal.toString())
        if (rc == null || !rc.clientAuthenticationMethods.contains(ClientAuthenticationMethod.NONE)) {
            throw OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT)
        }
        return OAuth2ClientAuthenticationToken(rc, ClientAuthenticationMethod.NONE, null)
    }

    override fun supports(authentication: Class<*>): Boolean =
        OAuth2ClientAuthenticationToken::class.java.isAssignableFrom(authentication)
}
