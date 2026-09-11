package com.pettrip.album.service;

import com.pettrip.common.service.BadRequestException;

/** 없는 사진과 남의 사진을 구분하지 않는다. 구분하면 남의 사진 id가 존재하는지 알려주는 셈이 된다. */
public class PhotoNotOwnedException extends BadRequestException {

  public PhotoNotOwnedException() {
    super("존재하지 않거나 본인의 사진이 아닙니다.");
  }
}
