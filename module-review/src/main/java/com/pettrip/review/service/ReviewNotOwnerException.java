package com.pettrip.review.service;

import com.pettrip.common.service.ForbiddenException;

public class ReviewNotOwnerException extends ForbiddenException {

  public ReviewNotOwnerException() {
    super("본인이 작성한 리뷰만 삭제할 수 있습니다.");
  }
}
