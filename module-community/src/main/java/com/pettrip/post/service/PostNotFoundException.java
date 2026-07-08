package com.pettrip.post.service;

import com.pettrip.common.service.NotFoundException;

public class PostNotFoundException extends NotFoundException {

  public PostNotFoundException() {
    super("게시글을 찾을 수 없습니다.");
  }
}
