package com.pettrip.pet.service;

import com.pettrip.common.service.NotFoundException;

public class PetNotFoundException extends NotFoundException {

  public PetNotFoundException() {
    super("반려견을 찾을 수 없습니다.");
  }
}
