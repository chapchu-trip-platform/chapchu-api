package com.pettrip.user.service;

import com.pettrip.common.service.NotFoundException;

public class RegionNotFoundException extends NotFoundException {

  public RegionNotFoundException() {
    super("지역을 찾을 수 없습니다.");
  }
}
