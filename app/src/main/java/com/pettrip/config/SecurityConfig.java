package com.pettrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * docs/decisions/025 참고: chapchu-auth(별도 레포)가 발급한 JWT를 OAuth2 Resource Server로 검증한다. 공개 엔드포인트(헬스체크,
 * API 문서, 장소/날씨 조회)를 제외한 모든 요청은 인증을 요구한다. 서명 검증에 쓰이는 JwtDecoder는 application.yml의
 * issuer-uri(AUTH_SERVER_URL) 기반 OIDC 디스커버리로 자동 구성된다.
 */
@Configuration
public class SecurityConfig {

  private static final String[] PUBLIC_PATHS = {
    "/actuator/health", "/actuator/health/**", "/docs/**"
  };

  private static final String[] PUBLIC_GET_PATHS = {"/places", "/places/**", "/weather"};

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }
}
