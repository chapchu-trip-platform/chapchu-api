package com.pettrip.trip.service;

import com.pettrip.common.service.NotFoundException;

public class NoPlacesFoundException extends NotFoundException {

  public NoPlacesFoundException() {
    super("주변에 반려동물 동반 가능 장소가 없습니다.");
  }
}
