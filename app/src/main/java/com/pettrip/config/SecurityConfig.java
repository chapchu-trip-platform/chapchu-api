package com.pettrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * docs/decisions/011 참고: chapchu-auth(별도 레포)의 OAuth2 Resource Server 연동이 붙기 전까지의 임시 설정. 모든 요청을
 * permitAll 처리한다. auth 도메인 구현 시 이 설정을 실제 JWT 검증으로 교체해야 한다.
 */
@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
