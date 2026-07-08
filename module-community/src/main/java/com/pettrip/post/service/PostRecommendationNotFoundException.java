package com.pettrip.post.service;

import com.pettrip.common.service.NotFoundException;

public class PostRecommendationNotFoundException extends NotFoundException {

  public PostRecommendationNotFoundException() {
    super("추천 내역을 찾을 수 없습니다.");
  }
}
