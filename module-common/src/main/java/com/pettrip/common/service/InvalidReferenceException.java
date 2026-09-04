package com.pettrip.common.service;

/**
 * 요청 본문이 가리키는 다른 자원이 없거나 요청한 사용자의 것이 아닐 때 던진다.
 *
 * <p>"없음"과 "남의 것"을 구분하지 않는다. 구분하면 남의 자원 ID가 존재하는지 알려주는 셈이 된다. 대신 어느 필드가 문제인지는 알려줘야 프론트가 고칠 수 있다.
 */
public class InvalidReferenceException extends BadRequestException {

  private final String field;

  public InvalidReferenceException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
