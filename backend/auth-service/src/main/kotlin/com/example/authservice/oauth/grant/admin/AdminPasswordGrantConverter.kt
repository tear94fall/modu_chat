package com.example.authservice.oauth.grant.admin

import com.example.authservice.oauth.config.AuthorizationServerConfig
import com.example.authservice.oauth.grant.GrantRequestParser
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationConverter

/** POST /oauth2/token grant_type=…admin_password&client_id=modu-admin&email=…&password=… (비밀번호는 저장하지 않는다) */
class AdminPasswordGrantConverter : AuthenticationConverter {

    override fun convert(request: HttpServletRequest): Authentication? {
        if (!GrantRequestParser.isGrant(request, AuthorizationServerConfig.ADMIN_PASSWORD.value)) {
            return null
        }
        val client = GrantRequestParser.clientPrincipal()
        val email = GrantRequestParser.required(request, "email")
        val password = GrantRequestParser.required(request, "password")
        return AdminPasswordGrantToken(client, email, password, GrantRequestParser.scopes(request), emptyMap())
    }
}
