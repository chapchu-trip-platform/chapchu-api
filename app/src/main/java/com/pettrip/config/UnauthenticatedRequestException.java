package com.pettrip.config;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * 인증이 필요한 핸들러에서 유저 UUID를 얻을 수 없을 때 던진다.
 *
 * <p>{@code AuthenticationException} 계열이므로 Spring Security의 {@code ExceptionTranslationFilter}가 401
 * 응답으로 변환한다. {@code SecurityConfig}가 이미 인증을 요구하므로 정상 흐름에서는 발생하지 않고, 설정 누락에 대한 안전망 역할이다.
 */
public class UnauthenticatedRequestException extends AuthenticationCredentialsNotFoundException {

  private static final long serialVersionUID = 1L;

  public UnauthenticatedRequestException(String message) {
    super(message);
  }
}
