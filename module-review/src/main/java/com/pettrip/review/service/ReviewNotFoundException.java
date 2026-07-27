package com.pettrip.review.service;

import com.pettrip.common.service.NotFoundException;

public class ReviewNotFoundException extends NotFoundException {

  public ReviewNotFoundException() {
    super("리뷰를 찾을 수 없습니다.");
  }
}
