package com.pettrip.pet.service;

import com.pettrip.common.service.NotFoundException;

public class BreedNotFoundException extends NotFoundException {

  public BreedNotFoundException() {
    super("견종을 찾을 수 없습니다.");
  }
}
