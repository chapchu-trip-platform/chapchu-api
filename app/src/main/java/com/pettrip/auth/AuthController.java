package com.pettrip.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final String REGISTRATION_ID = "chapchu-auth";
  private static final String STATE_COOKIE = "oauth2_state";
  private static final String REDIRECT_COOKIE = "oauth2_redirect";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  private final ClientRegistrationRepository clientRegistrationRepository;
  private final RestClient restClient = RestClient.create();

  @Value("${chapchu-api.auth.fe-redirect-url}")
  private String feRedirectUrl;

  @Value("${chapchu-api.auth.callback-url:}")
  private String configuredCallbackUrl;

  @Value("${cors.allowed-origins:http://localhost:3000}")
  private String allowedOrigins;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String authServerUrl;

  public AuthController(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /** FE 로그인 진입점. redirect 파라미터로 환경별 콜백 URL 지정 가능. 허용된 CORS origin 기준으로 검증하여 open redirect를 차단한다. */
  @GetMapping("/login")
  public void login(
      @RequestParam(required = false) String redirect,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
    String state = UUID.randomUUID().toString();

    Cookie stateCookie = new Cookie(STATE_COOKIE, state);
    stateCookie.setHttpOnly(true);
    stateCookie.setSecure(request.isSecure());
    stateCookie.setPath("/");
    stateCookie.setMaxAge(300);
    response.addCookie(stateCookie);

    String resolvedRedirect = resolveRedirectUrl(redirect);
    Cookie redirectCookie = new Cookie(REDIRECT_COOKIE, resolvedRedirect);
    redirectCookie.setHttpOnly(true);
    redirectCookie.setSecure(request.isSecure());
    redirectCookie.setPath("/auth/callback");
    redirectCookie.setMaxAge(300);
    response.addCookie(redirectCookie);

    String callbackUri = buildRedirectUri(request);
    String authorizationUri =
        reg.getProviderDetails().getAuthorizationUri()
            + "?response_type=code"
            + "&client_id="
            + reg.getClientId()
            + "&redirect_uri="
            + URLEncoder.encode(callbackUri, StandardCharsets.UTF_8)
            + "&scope="
            + URLEncoder.encode(String.join(" ", reg.getScopes()), StandardCharsets.UTF_8)
            + "&state="
            + state
            + "&prompt=select_account";

    response.sendRedirect(authorizationUri);
  }

  /**
   * chapchu-auth 콜백.
   *
   * <ul>
   *   <li>신규 유저: {@code ?registration_token=xxx} → FE 콜백으로 리다이렉트
   *   <li>기존 유저: {@code ?code=xxx&state=xxx} → 토큰 교환 → FE 콜백으로 리다이렉트
   * </ul>
   */
  @GetMapping("/callback")
  public void callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(name = "registration_token", required = false) String registrationToken,
      @CookieValue(name = STATE_COOKIE, required = false) String stateCookie,
      @CookieValue(name = REDIRECT_COOKIE, required = false) String redirectCookie,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    String targetRedirect = redirectCookie != null ? redirectCookie : feRedirectUrl;
    clearCookie(response, REDIRECT_COOKIE, "/auth/callback");

    if (registrationToken != null) {
      response.sendRedirect(targetRedirect + "?registration_token=" + registrationToken);
      return;
    }

    if (code == null
        || code.isEmpty()
        || state == null
        || stateCookie == null
        || !state.equals(stateCookie)) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "state 불일치 또는 code 없음");
      return;
    }

    clearCookie(response, STATE_COOKIE, "/");

    TokenResponse tokens = exchangeCode(code, buildRedirectUri(request));
    setRefreshTokenCookie(response, tokens.refreshToken(), request.isSecure());
    response.sendRedirect(targetRedirect + "#access_token=" + tokens.accessToken());
  }

  /** refresh_token을 auth 서버에서 폐기하고 쿠키를 만료시켜 로그아웃한다. */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
      HttpServletResponse response) {
    if (refreshToken != null) {
      revokeToken(refreshToken);
    }
    clearCookie(response, REFRESH_TOKEN_COOKIE, "/auth/refresh");
    return ResponseEntity.ok().build();
  }

  private void revokeToken(String refreshToken) {
    try {
      ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
      String credentials =
          Base64.getEncoder()
              .encodeToString(
                  (reg.getClientId() + ":" + reg.getClientSecret())
                      .getBytes(StandardCharsets.UTF_8));
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("token", refreshToken);
      body.add("token_type_hint", "refresh_token");
      restClient
          .post()
          .uri(URI.create(authServerUrl + "/oauth2/revoke"))
          .header("Authorization", "Basic " + credentials)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(body)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ignored) {
      // revoke 실패해도 클라이언트 쿠키는 무조건 삭제
    }
  }

  /** refresh_token 쿠키로 새 access_token을 발급한다. 토큰 회전 시 새 refresh_token도 쿠키에 갱신한다. */
  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
      HttpServletRequest request,
      HttpServletResponse response) {
    if (refreshToken == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "refresh_token");
    body.add("refresh_token", refreshToken);

    String credentials =
        Base64.getEncoder()
            .encodeToString(
                (reg.getClientId() + ":" + reg.getClientSecret()).getBytes(StandardCharsets.UTF_8));

    Map<?, ?> tokenResponse;
    try {
      tokenResponse =
          restClient
              .post()
              .uri(URI.create(authServerUrl + "/oauth2/token"))
              .header("Authorization", "Basic " + credentials)
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(body)
              .retrieve()
              .onStatus(
                  status -> status.is4xxClientError(),
                  (req, res) -> {
                    throw new org.springframework.web.client.HttpClientErrorException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED);
                  })
              .body(Map.class);
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    String newRefreshToken = (String) tokenResponse.get("refresh_token");
    if (newRefreshToken != null) {
      setRefreshTokenCookie(response, newRefreshToken, request.isSecure());
    }

    return ResponseEntity.ok(Map.of("access_token", (String) tokenResponse.get("access_token")));
  }

  /** redirect 파라미터가 허용된 origin 기준으로 유효하면 그대로, 아니면 기본 URL 반환. */
  private String resolveRedirectUrl(String redirect) {
    if (redirect == null || redirect.isBlank()) {
      return feRedirectUrl;
    }
    boolean allowed =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .anyMatch(origin -> redirect.startsWith(origin + "/") || redirect.equals(origin));
    return allowed ? redirect : feRedirectUrl;
  }

  private TokenResponse exchangeCode(String code, String redirectUri) {
    ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "authorization_code");
    body.add("code", code);
    body.add("redirect_uri", redirectUri);

    String credentials =
        Base64.getEncoder()
            .encodeToString(
                (reg.getClientId() + ":" + reg.getClientSecret()).getBytes(StandardCharsets.UTF_8));

    Map<?, ?> tokenResponse =
        restClient
            .post()
            .uri(URI.create(authServerUrl + "/oauth2/token"))
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(Map.class);

    return new TokenResponse(
        (String) tokenResponse.get("access_token"), (String) tokenResponse.get("refresh_token"));
  }

  private String buildRedirectUri(HttpServletRequest request) {
    if (configuredCallbackUrl != null && !configuredCallbackUrl.isEmpty()) {
      return configuredCallbackUrl;
    }
    int port = request.getServerPort();
    String portPart = (port == 80 || port == 443) ? "" : ":" + port;
    return request.getScheme() + "://" + request.getServerName() + portPart + "/auth/callback";
  }

  private void setRefreshTokenCookie(
      HttpServletResponse response, String refreshToken, boolean secure) {
    Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
    cookie.setHttpOnly(true);
    cookie.setSecure(secure);
    cookie.setPath("/auth/refresh");
    cookie.setMaxAge(14 * 24 * 60 * 60);
    response.addCookie(cookie);
  }

  private void clearCookie(HttpServletResponse response, String name, String path) {
    Cookie cookie = new Cookie(name, "");
    cookie.setPath(path);
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  private record TokenResponse(String accessToken, String refreshToken) {}
}
