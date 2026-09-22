package com.example.authservice.oauth.grant.sso

import com.example.authservice.oauth.config.AuthorizationServerConfig
import com.example.authservice.oauth.grant.GrantRequestParser
import com.example.authservice.oauth.grant.GrantSupport
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationConverter

/** POST /oauth2/token grant_type=…sso_code&client_id=…&code=…&code_verifier=… */
class SsoCodeGrantConverter : AuthenticationConverter {

    override fun convert(request: HttpServletRequest): Authentication? {
        if (!GrantRequestParser.isGrant(request, AuthorizationServerConfig.SSO_CODE.value)) {
            return null
        }
        val client = GrantRequestParser.clientPrincipal()
        val code = GrantRequestParser.required(request, "code")
        val verifier = GrantRequestParser.required(request, "code_verifier")
        return SsoCodeGrantToken(
            client, code, verifier, GrantRequestParser.scopes(request),
            GrantSupport.additionalParameters(request.parameterMap),
        )
    }
}
