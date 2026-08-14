package com.pettrip.common.service;

/** 요청자가 자기 자신을 증명하지 못했을 때. 토큰이 없거나·위조됐거나·만료된 경우다. */
public abstract class UnauthorizedException extends RuntimeException {

  protected UnauthorizedException(String message) {
    super(message);
  }
}
