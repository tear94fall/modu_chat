package com.example.authservice.auth.config;

import com.example.authservice.auth.jwt.JwtTokenProvider;
import com.example.authservice.security.filter.CustomAuthenticationFilter;
import com.example.authservice.security.handler.CustomAuthenticationFailureHandler;
import com.example.authservice.security.handler.CustomAuthenticationSuccessHandler;
import com.example.authservice.security.provider.CustomAuthenticationProvider;
import com.example.authservice.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CustomAuthenticationFilter customAuthenticationFilter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("X-Requested-With", "Content-Type", "Authorization", "X-XSRF-token"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CustomAuthenticationFilter customAuthenticationFilter(
            AuthenticationManager authenticationManager,
            CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
            CustomAuthenticationFailureHandler customAuthenticationFailureHandler
    ) {
        CustomAuthenticationFilter customAuthenticationFilter = new CustomAuthenticationFilter(authenticationManager, customAuthenticationSuccessHandler, customAuthenticationFailureHandler);
        customAuthenticationFilter.setFilterProcessesUrl("/login");
        customAuthenticationFilter.afterPropertiesSet();
        return customAuthenticationFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomAuthenticationProvider customAuthenticationProvider) {
        return new ProviderManager(Collections.singletonList(customAuthenticationProvider));
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider();
    }

    @Bean
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() throws Exception {
        return new CustomAuthenticationSuccessHandler(jwtTokenProvider, refreshTokenService);
    }

    @Bean
    public CustomAuthenticationFailureHandler customAuthenticationFailureHandler() throws Exception {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
