package com.pettrip.config;

import com.pettrip.common.service.AccountWithdrawnException;
import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @CurrentUserId UUID} 파라미터에 인증된 유저의 UUID를 주입한다.
 *
 * <p>docs/decisions/026 참고: JWT의 {@code sub} 클레임이 {@code users.user_id}(UUID v7 문자열)라는 decision 016
 * 계약에 의존한다. 추출 로직을 이 한 곳에 모아 두어 도메인 모듈이 Spring Security에 의존하지 않게 한다.
 *
 * <p>docs/decisions/049 참고: 여기서 계정 상태도 함께 확인한다. access token은 chapchu-auth가 서명한 JWT라 이 저장소에서 만료시킬 수
 * 없어, 탈퇴한 계정의 토큰은 유저 id를 내주지 않는 방식으로 막는다. 인증이 필요한 엔드포인트는 전부 이 리졸버를 거치므로 한 곳만 막으면 된다.
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

  private final UserService userService;

  public CurrentUserIdArgumentResolver(UserService userService) {
    this.userService = userService;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    if (!parameter.hasParameterAnnotation(CurrentUserId.class)) {
      return false;
    }
    return UUID.class.equals(parameter.getParameterType())
        || Optional.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    boolean optional = Optional.class.equals(parameter.getParameterType());
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      if (optional) {
        return Optional.empty();
      }
      throw new UnauthenticatedRequestException("인증된 JWT 정보가 없습니다.");
    }

    String subject = jwtAuthentication.getToken().getSubject();
    UUID userId = parseUserId(subject);
    if (optional) {
      return optionalUserId(userId);
    }
    if (!userService.isActive(userId)) {
      throw new AccountWithdrawnException();
    }
    return userId;
  }

  private UUID parseUserId(String subject) {
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new UnauthenticatedRequestException("JWT sub 클레임이 유효한 UUID가 아닙니다.");
    }
  }

  /**
   * {@code Optional} 변형은 공개 엔드포인트에서 "로그인했으면 추가 정보"를 붙이는 데 쓴다(예: 장소 상세의 찜 여부). 탈퇴 계정은 막는 대신 비로그인으로
   * 취급한다 — 토큰 상태와 무관하게 공개 API는 열려 있어야 한다는 게 팀 합의다(docs/failures/028).
   */
  private Optional<UUID> optionalUserId(UUID userId) {
    if (!userService.isActive(userId)) {
      return Optional.empty();
    }
    return Optional.of(userId);
  }
}
