package com.pettrip.pet.service;

import com.pettrip.common.service.NotFoundException;

public class PetActivityNotFoundException extends NotFoundException {

  public PetActivityNotFoundException() {
    super("활동 유형을 찾을 수 없습니다.");
  }
}
