package com.pettrip.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private static final String REGISTRATION_ID = "chapchu-auth";
  private static final String STATE_COOKIE = "oauth2_state";
  private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

  private final ClientRegistrationRepository clientRegistrationRepository;
  private final RestClient restClient = RestClient.create();

  @Value("${chapchu-api.auth.fe-callback-url}")
  private String feCallbackUrl;

  @Value("${chapchu-api.auth.fe-onboarding-url}")
  private String feOnboardingUrl;

  @Value("${chapchu-api.auth.callback-url:}")
  private String configuredCallbackUrl;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String authServerUrl;

  public AuthController(ClientRegistrationRepository clientRegistrationRepository) {
    this.clientRegistrationRepository = clientRegistrationRepository;
  }

  /** FE 로그인 진입점. state를 쿠키에 저장하고 chapchu-auth 인가 엔드포인트로 리다이렉트한다. */
  @GetMapping("/login")
  public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ClientRegistration reg = clientRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
    String state = UUID.randomUUID().toString();

    Cookie stateCookie = new Cookie(STATE_COOKIE, state);
    stateCookie.setHttpOnly(true);
    stateCookie.setSecure(request.isSecure());
    stateCookie.setPath("/");
    stateCookie.setMaxAge(300);
    response.addCookie(stateCookie);

    String redirectUri = buildRedirectUri(request);
    String authorizationUri =
        reg.getProviderDetails().getAuthorizationUri()
            + "?response_type=code"
            + "&client_id="
            + reg.getClientId()
            + "&redirect_uri="
            + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
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
   *   <li>신규 유저: {@code ?registration_token=xxx} → FE 온보딩 페이지로 리다이렉트
   *   <li>기존 유저: {@code ?code=xxx&state=xxx} → 토큰 교환 → FE 콜백으로 리다이렉트
   * </ul>
   */
  @GetMapping("/callback")
  public void callback(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(name = "registration_token", required = false) String registrationToken,
      @CookieValue(name = STATE_COOKIE, required = false) String stateCookie,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {

    if (registrationToken != null) {
      response.sendRedirect(feOnboardingUrl + "?registration_token=" + registrationToken);
      return;
    }

    if (code == null || !code.isEmpty() && (state == null || !state.equals(stateCookie))) {
      response.sendError(HttpStatus.BAD_REQUEST.value(), "state 불일치 또는 code 없음");
      return;
    }

    clearCookie(response, STATE_COOKIE);

    TokenResponse tokens = exchangeCode(code, buildRedirectUri(request));
    setRefreshTokenCookie(response, tokens.refreshToken(), request.isSecure());
    response.sendRedirect(feCallbackUrl + "#access_token=" + tokens.accessToken());
  }

  /** refresh_token 쿠키로 새 access_token을 발급한다. */
  @PostMapping("/refresh")
  public ResponseEntity<Map<String, String>> refresh(
      @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
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

    Map<?, ?> tokenResponse =
        restClient
            .post()
            .uri(URI.create(authServerUrl + "/oauth2/token"))
            .header("Authorization", "Basic " + credentials)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(Map.class);

    return ResponseEntity.ok(Map.of("access_token", (String) tokenResponse.get("access_token")));
  }

  /** chapchu-auth POST /auth/register 프록시. FE가 직접 chapchu-auth를 호출하지 않는다. */
  @PostMapping("/register")
  public ResponseEntity<Void> register(@RequestBody Map<String, String> body) {
    restClient
        .post()
        .uri(URI.create(authServerUrl + "/auth/register"))
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .toBodilessEntity();
    return ResponseEntity.ok().build();
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

  private void clearCookie(HttpServletResponse response, String name) {
    Cookie cookie = new Cookie(name, "");
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  private record TokenResponse(String accessToken, String refreshToken) {}
}
