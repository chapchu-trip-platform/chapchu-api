package com.pettrip.post.service;

import com.pettrip.common.service.ConflictException;

public class PostAlreadyReportedException extends ConflictException {

  public PostAlreadyReportedException() {
    super("이미 신고한 게시글입니다.");
  }
}
