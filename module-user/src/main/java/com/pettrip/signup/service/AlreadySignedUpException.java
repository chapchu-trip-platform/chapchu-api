package com.pettrip.signup.service;

import com.pettrip.common.service.ConflictException;

public class AlreadySignedUpException extends ConflictException {

  public AlreadySignedUpException() {
    super("이미 가입된 계정입니다.");
  }
}
