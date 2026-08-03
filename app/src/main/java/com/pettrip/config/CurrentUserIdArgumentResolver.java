package com.pettrip.config;

import com.pettrip.common.service.CurrentUserId;
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
 */
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserId.class)
        && UUID.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
      throw new UnauthenticatedRequestException("인증된 JWT 정보가 없습니다.");
    }

    String subject = jwtAuthentication.getToken().getSubject();
    try {
      return UUID.fromString(subject);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new UnauthenticatedRequestException("JWT sub 클레임이 유효한 UUID가 아닙니다.");
    }
  }
}
