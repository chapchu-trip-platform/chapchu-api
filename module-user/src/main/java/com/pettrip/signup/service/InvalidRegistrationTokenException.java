package com.pettrip.signup.service;

import com.pettrip.common.service.UnauthorizedException;

public class InvalidRegistrationTokenException extends UnauthorizedException {

  public InvalidRegistrationTokenException() {
    super("가입 토큰이 유효하지 않습니다. 다시 로그인해주세요.");
  }
}
