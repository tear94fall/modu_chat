package com.example.pointservice.api.config

import com.example.pointservice.api.common.AuthUserInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(private val authUserInterceptor: AuthUserInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authUserInterceptor).addPathPatterns(AuthUserInterceptor.PUBLIC_PATH_PATTERN)
    }
}
