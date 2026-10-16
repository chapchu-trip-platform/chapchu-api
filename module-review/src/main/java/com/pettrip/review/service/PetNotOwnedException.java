package com.pettrip.review.service;

import com.pettrip.common.service.ForbiddenException;

public class PetNotOwnedException extends ForbiddenException {

  public PetNotOwnedException() {
    super("본인의 반려동물로만 리뷰를 작성할 수 있습니다.");
  }
}
