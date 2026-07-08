package com.pettrip.post.service;

import com.pettrip.common.service.NotFoundException;

public class PostBookmarkNotFoundException extends NotFoundException {

  public PostBookmarkNotFoundException() {
    super("북마크 내역을 찾을 수 없습니다.");
  }
}
