package com.pettrip.signup.service;

import com.pettrip.common.service.ExternalApiException;

/** 인증 서버에 닿지 못했다. 토큰이 잘못된 것과 구분해야 사용자에게 "다시 로그인"이 아니라 "잠시 후 재시도"를 안내할 수 있다. */
public class RegistrationTokenVerificationFailedException extends ExternalApiException {

  public RegistrationTokenVerificationFailedException() {
    super("인증 서버에 연결하지 못했습니다. 잠시 후 다시 시도해주세요.");
  }
}
