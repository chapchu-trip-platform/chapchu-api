package com.pettrip.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * @param message 사람이 읽는 한 줄 요약. 첫 번째 위반을 담는다
 * @param fieldErrors 위반된 필드 전체. 필드를 특정할 수 없는 오류에서는 내려가지 않는다
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {

  public ErrorResponse(String code, String message) {
    this(code, message, null);
  }

  public record FieldError(String field, String message) {}
}
