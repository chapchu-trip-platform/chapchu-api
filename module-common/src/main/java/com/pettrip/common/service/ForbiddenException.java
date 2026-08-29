package com.pettrip.common.service;

public abstract class ForbiddenException extends RuntimeException {

  protected ForbiddenException(String message) {
    super(message);
  }
}
