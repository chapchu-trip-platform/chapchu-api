package com.pettrip.user.service;

import com.pettrip.common.service.NotFoundException;

public class TransportMethodNotFoundException extends NotFoundException {

  public TransportMethodNotFoundException() {
    super("이동 수단을 찾을 수 없습니다.");
  }
}
