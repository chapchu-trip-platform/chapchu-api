package com.pettrip.common.service;

public abstract class ConflictException extends RuntimeException {

  protected ConflictException(String message) {
    super(message);
  }
}
