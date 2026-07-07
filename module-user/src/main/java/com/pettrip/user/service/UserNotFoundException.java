package com.pettrip.user.service;

import com.pettrip.common.service.NotFoundException;

public class UserNotFoundException extends NotFoundException {

  public UserNotFoundException() {
    super("유저를 찾을 수 없습니다.");
  }
}
