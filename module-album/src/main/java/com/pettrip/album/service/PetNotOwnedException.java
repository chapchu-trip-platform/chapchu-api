package com.pettrip.album.service;

import com.pettrip.common.service.BadRequestException;

/** 없는 반려동물과 남의 반려동물을 구분하지 않는다. 구분하면 남의 id가 존재하는지 알려주는 셈이 된다. */
public class PetNotOwnedException extends BadRequestException {

  public PetNotOwnedException() {
    super("존재하지 않거나 본인의 반려동물이 아닙니다.");
  }
}
