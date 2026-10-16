package com.pettrip.common.service;

/**
 * 우리가 의존하는 바깥 서비스가 응답하지 않을 때.
 *
 * <p>사용자 잘못이 아니므로 4xx로 돌려주면 안 되고, 우리 서버가 망가진 것도 아니므로 500도 정확하지 않다. 502로 구분해 프론트가 "잠시 후 다시 시도"를 안내할
 * 수 있게 한다.
 */
public abstract class ExternalApiException extends RuntimeException {

  protected ExternalApiException(String message) {
    super(message);
  }
}
