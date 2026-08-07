package com.pettrip.common.service;

/**
 * 외부 API 연동 실패. 우리 잘못이 아니라 상대 서비스가 응답하지 않거나 설정이 빠진 경우다.
 *
 * <p>{@code GlobalExceptionHandler}가 502 Bad Gateway로 변환한다. 500으로 두면 "우리 서버가 깨졌다"는 뜻이 되어, 외부 장애와 내부
 * 버그를 구분할 수 없다.
 */
public class ExternalApiException extends RuntimeException {

  public ExternalApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
