package com.pettrip.user.service;

import com.pettrip.common.service.ConflictException;

public class NicknameAlreadyRegisteredException extends ConflictException {

  public NicknameAlreadyRegisteredException() {
    super("이미 닉네임이 등록되어 있습니다.");
  }
}
