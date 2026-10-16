package com.pettrip.signup.service;

import java.net.URI;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * chapchu-auth에 registration token 검증을 맡긴다.
 *
 * <p>토큰을 서명한 HMAC 비밀키는 chapchu-auth에만 있다. 비밀키를 이 레포에 복사하면 키를 회전할 때 반드시 한쪽이 뒤처지고, 그러면 로그인한 신규 유저가 가입
 * 단계에서 막힌다.
 *
 * <p>검증 엔드포인트는 <b>읽기 전용</b>이라 이 서버가 뒤이어 실패해도 저쪽에 되돌릴 상태가 남지 않는다. 사용자는 같은 토큰으로 그대로 재시도할 수 있다.
 * chapchu-auth의 {@code POST /auth/register}를 대신 부르면 유저가 이미 만들어져 재시도가 깨진다.
 */
@Component
public class RegistrationTokenClient {

  private final RestClient restClient = RestClient.create();
  private final String authServerUrl;

  public RegistrationTokenClient(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String authServerUrl) {
    this.authServerUrl = authServerUrl;
  }

  /**
   * 토큰을 검증하고 담긴 구글 계정 정보를 돌려준다.
   *
   * <p>chapchu-auth가 <b>401을 줄 때만</b> 토큰이 잘못된 것으로 본다. 형식 오류·서명 불일치·만료가 모두 401로 온다.
   *
   * <p>그 밖의 응답(403·404 등)은 사용자 잘못이 아니라 <b>배포가 어긋난 것</b>이다. 검증 엔드포인트가 아직 안 올라갔거나 경로가 막혀 있을 때 그렇다. 이걸
   * "토큰이 유효하지 않습니다"로 안내하면 사용자는 다시 로그인해도 계속 막히고, 원인을 찾을 단서가 없다. 502로 구분해 "잠시 후 다시 시도"를 안내한다.
   */
  public VerifiedRegistration verify(String registrationToken) {
    try {
      Map<?, ?> body =
          restClient
              .post()
              .uri(URI.create(authServerUrl + "/auth/registration-token/verify"))
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("registrationToken", registrationToken))
              .retrieve()
              .onStatus(
                  status -> status.value() == HttpStatus.UNAUTHORIZED.value(),
                  (request, response) -> {
                    throw new InvalidRegistrationTokenException();
                  })
              .onStatus(
                  HttpStatusCode::isError,
                  (request, response) -> {
                    throw new RegistrationTokenVerificationFailedException();
                  })
              .body(Map.class);

      if (body == null || body.get("googleUserId") == null || body.get("email") == null) {
        throw new RegistrationTokenVerificationFailedException();
      }
      return new VerifiedRegistration(
          (String) body.get("googleUserId"), (String) body.get("email"));

    } catch (InvalidRegistrationTokenException | RegistrationTokenVerificationFailedException e) {
      throw e;
    } catch (RestClientException e) {
      throw new RegistrationTokenVerificationFailedException();
    }
  }

  public record VerifiedRegistration(String googleUserId, String email) {}
}
