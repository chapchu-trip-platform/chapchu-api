package com.pettrip.common.service;

public abstract class BadRequestException extends RuntimeException {

  protected BadRequestException(String message) {
    super(message);
  }
}
