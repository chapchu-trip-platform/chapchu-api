package com.pettrip.config;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
 *
 * <p>필터체인을 둘로 분리한 이유: oauth2ResourceServer는 Bearer 토큰이 있으면 무조건 검증하므로, 만료된 토큰을 들고 공개 엔드포인트에 접근해도
 * 401이 떴다. 공개 경로는 JWT 필터 자체를 타지 않는 별도 체인으로 분리하여 토큰 유무·유효성과 무관하게 항상 200을 반환한다.
 */
@Configuration
public class SecurityConfig {

  /**
   * 인증 없이 열린 경로. 인증 없이 조회할 수 있는 GET 경로.
   *
   * <p>{@code /breeds}, {@code /activities}, {@code /preferences/options}는 온보딩 때문에 열어 둔다. 신규 유저는
   * 회원가입 화면에서 선호 사항과 반려동물을 고르는데, 그 시점에는 아직 access token이 없고 registration token만 갖고 있다. 유저와 무관한 고정
   * 코드값이라 공개해도 드러나는 정보가 없다.
   */
  private static final String[] PUBLIC_POST_PATHS = {
    "/auth/login",
    "/auth/callback",
    "/auth/register",
    "/auth/signup",
    "/auth/refresh",
    "/auth/logout"
  };

  private static final String[] PUBLIC_ANY_PATHS = {
    "/actuator/health", "/actuator/health/**", "/docs/**"
  };

  private static final String[] PUBLIC_GET_PATHS = {
    "/places", "/places/**", "/weather", "/breeds", "/activities", "/preferences/options"
  };

  /** 공개 체인: JWT 필터 없음. 만료 토큰을 들고 와도 정상 응답한다. */
  @Bean
  @Order(1)
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(
            request -> {
              String method = request.getMethod();
              String path = request.getRequestURI();
              if (matchesAny(path, PUBLIC_ANY_PATHS)) return true;
              if ("POST".equals(method) && matchesAny(path, PUBLIC_POST_PATHS)) return true;
              if ("GET".equals(method) && matchesAny(path, PUBLIC_GET_PATHS)) return true;
              if (DispatcherType.ERROR == request.getDispatcherType()) return true;
              if (DispatcherType.FORWARD == request.getDispatcherType()) return true;
              return false;
            })
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }

  /** 인증 체인: JWT 필수. 유효하지 않은 토큰은 401 반환한다. */
  @Bean
  @Order(2)
  public SecurityFilterChain privateFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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

  private boolean matchesAny(String path, String[] patterns) {
    for (String pattern : patterns) {
      if (pattern.endsWith("/**")) {
        if (path.startsWith(pattern.substring(0, pattern.length() - 3))) return true;
      } else {
        if (path.equals(pattern)) return true;
      }
    }
    return false;
  }
}
