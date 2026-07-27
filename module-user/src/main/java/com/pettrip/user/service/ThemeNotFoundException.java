package com.pettrip.user.service;

import com.pettrip.common.service.NotFoundException;

public class ThemeNotFoundException extends NotFoundException {

  public ThemeNotFoundException() {
    super("테마를 찾을 수 없습니다.");
  }
}
