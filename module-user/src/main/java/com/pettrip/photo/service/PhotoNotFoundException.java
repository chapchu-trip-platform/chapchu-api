package com.pettrip.photo.service;

import com.pettrip.common.service.NotFoundException;

/** 사진이 없거나 요청자의 것이 아닐 때. "없음"과 "남의 것"을 구분하지 않는다(IDOR 방지). */
public class PhotoNotFoundException extends NotFoundException {

  public PhotoNotFoundException() {
    super("사진을 찾을 수 없습니다.");
  }
}
