package com.pettrip.trip.service;

import com.pettrip.common.service.ForbiddenException;

public class CourseNotOwnerException extends ForbiddenException {

  public CourseNotOwnerException() {
    super("본인의 코스만 접근할 수 있습니다.");
  }
}
