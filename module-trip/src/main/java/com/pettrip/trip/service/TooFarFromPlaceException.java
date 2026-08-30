package com.pettrip.trip.service;

import com.pettrip.common.service.BadRequestException;

public class TooFarFromPlaceException extends BadRequestException {

  public TooFarFromPlaceException() {
    super("현재 위치가 장소에서 500m 이상 떨어져 있습니다.");
  }
}
