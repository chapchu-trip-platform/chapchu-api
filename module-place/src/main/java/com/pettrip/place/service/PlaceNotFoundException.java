package com.pettrip.place.service;

import com.pettrip.common.service.NotFoundException;

public class PlaceNotFoundException extends NotFoundException {

  public PlaceNotFoundException() {
    super("장소를 찾을 수 없습니다.");
  }
}
