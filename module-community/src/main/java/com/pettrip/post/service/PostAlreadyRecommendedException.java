package com.pettrip.post.service;

import com.pettrip.common.service.ConflictException;

public class PostAlreadyRecommendedException extends ConflictException {

  public PostAlreadyRecommendedException() {
    super("이미 추천한 게시글입니다.");
  }
}
