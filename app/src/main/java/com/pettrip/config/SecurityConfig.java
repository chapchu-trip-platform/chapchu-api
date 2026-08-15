package com.pettrip.config;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * docs/decisions/025 참고: chapchu-auth(별도 레포)가 발급한 JWT를 OAuth2 Resource Server로 검증한다. 공개 엔드포인트(헬스체크,
 * API 문서, 장소/날씨 조회)를 제외한 모든 요청은 인증을 요구한다. 서명 검증에 쓰이는 JwtDecoder는 application.yml의
 * issuer-uri(AUTH_SERVER_URL) 기반 OIDC 디스커버리로 자동 구성된다.
 *
 * <p>ERROR/FORWARD 디스패치를 permitAll 하는 이유는 docs/failures/022 참고. 이걸 빠뜨리면 404가 {@code /error}로
 * forward될 때 인증 필터에 다시 걸려 모든 404가 401로 둔갑한다.
 */
@Configuration
public class SecurityConfig {

  public static final String[] PUBLIC_PATHS = {
    "/actuator/health",
    "/actuator/health/**",
    "/docs/**",
    "/auth/login",
    "/auth/callback",
    "/auth/register",
    "/auth/refresh",
    "/auth/logout",
    "/auth/signup"
  };

  /**
   * 인증 없이 조회할 수 있는 GET 경로.
   *
   * <p>{@code /breeds}, {@code /activities}, {@code /preferences/options}는 온보딩 때문에 열어 둔다. 신규 유저는
   * 회원가입 화면에서 선호 사항과 반려동물을 고르는데, 그 시점에는 아직 access token이 없고 registration token만 갖고 있다. 유저와 무관한 고정
   * 코드값이라 공개해도 드러나는 정보가 없다.
   */
  public static final String[] PUBLIC_GET_PATHS = {
    "/places",
    "/places/**",
    "/weather",
    "/breeds",
    "/activities",
    "/preferences/options",
    "/users/nickname/availability"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .requestMatchers(PUBLIC_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${cors.allowed-origins:http://localhost:3000}") String allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(false);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
