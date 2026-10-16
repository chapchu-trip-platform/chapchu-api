package com.pettrip.user.service;

import com.pettrip.common.service.ConflictException;

/** docs/decisions/027 참고: 다른 유저가 이미 사용 중인 닉네임을 등록/변경하려 할 때. */
public class NicknameAlreadyInUseException extends ConflictException {

  public NicknameAlreadyInUseException() {
    super("이미 사용 중인 닉네임입니다.");
  }
}
