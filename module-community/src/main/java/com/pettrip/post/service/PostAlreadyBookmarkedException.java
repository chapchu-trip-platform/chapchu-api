package com.pettrip.post.service;

import com.pettrip.common.service.ConflictException;

public class PostAlreadyBookmarkedException extends ConflictException {

  public PostAlreadyBookmarkedException() {
    super("이미 북마크한 게시글입니다.");
  }
}
