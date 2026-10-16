package com.pettrip.review.service;

import com.pettrip.common.service.NotFoundException;

public class ReviewRecommendationNotFoundException extends NotFoundException {

  public ReviewRecommendationNotFoundException() {
    super("추천 내역을 찾을 수 없습니다.");
  }
}
