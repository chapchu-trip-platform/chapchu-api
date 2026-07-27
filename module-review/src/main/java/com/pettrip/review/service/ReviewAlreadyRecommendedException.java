package com.pettrip.review.service;

import com.pettrip.common.service.ConflictException;

public class ReviewAlreadyRecommendedException extends ConflictException {

  public ReviewAlreadyRecommendedException() {
    super("이미 추천한 리뷰입니다.");
  }
}
