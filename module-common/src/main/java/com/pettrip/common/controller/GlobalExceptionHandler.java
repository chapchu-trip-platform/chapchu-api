package com.pettrip.common.controller;

import com.pettrip.common.model.ErrorResponse;
import com.pettrip.common.service.ConflictException;
import com.pettrip.common.service.NotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("NOT_FOUND", exception.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("CONFLICT", exception.getMessage()));
  }

  /**
   * {@code @Validated} 컨트롤러의 요청 파라미터 제약 위반. 요청 본문(DTO) 검증은 {@code
   * MethodArgumentNotValidException}으로 별도 처리된다.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    String message =
        exception.getConstraintViolations().stream()
            .map(violation -> lastPathNode(violation) + ": " + violation.getMessage())
            .findFirst()
            .orElse("잘못된 요청입니다.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", message));
  }

  private String lastPathNode(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot < 0 ? path : path.substring(lastDot + 1);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(
      MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("잘못된 요청입니다.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", message));
  }
}
