package com.pettrip.trip.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CourseNotFoundException extends RuntimeException {

  public CourseNotFoundException() {
    super("코스를 찾을 수 없습니다.");
  }
}
