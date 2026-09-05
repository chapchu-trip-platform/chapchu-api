package com.pettrip.common.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.pettrip.common.model.ErrorResponse;
import com.pettrip.common.service.BadRequestException;
import com.pettrip.common.service.ConflictException;
import com.pettrip.common.service.ExternalApiException;
import com.pettrip.common.service.ForbiddenException;
import com.pettrip.common.service.InvalidReferenceException;
import com.pettrip.common.service.NotFoundException;
import com.pettrip.common.service.UnauthorizedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("BAD_REQUEST", exception.getMessage()));
  }

  /** 요청 본문이 가리키는 자원이 없거나 남의 것일 때. 어느 필드가 문제인지 알려준다. */
  @ExceptionHandler(InvalidReferenceException.class)
  public ResponseEntity<ErrorResponse> handleInvalidReference(InvalidReferenceException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "INVALID_REQUEST",
                e.getField() + ": " + e.getMessage(),
                List.of(new ErrorResponse.FieldError(e.getField(), e.getMessage()))));
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("NOT_FOUND", exception.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("UNAUTHORIZED", e.getMessage()));
  }

  /** 바깥 서비스 장애를 500으로 흘리지 않는다. 프론트가 "잠시 후 다시 시도"를 안내할 수 있어야 한다. */
  @ExceptionHandler(ExternalApiException.class)
  public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException e) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("EXTERNAL_API_ERROR", e.getMessage()));
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("FORBIDDEN", e.getMessage()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ErrorResponse> handleConflict(ConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("CONFLICT", exception.getMessage()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", "입력 데이터가 올바르지 않습니다."));
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

  /** 위반된 필드를 하나만 알려주면 프론트가 왕복하며 고쳐야 한다. 전부 내려준다. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(
      MethodArgumentNotValidException exception) {
    List<ErrorResponse.FieldError> fieldErrors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
            .toList();
    String message =
        fieldErrors.stream()
            .map(error -> error.field() + ": " + error.message())
            .findFirst()
            .orElse("잘못된 요청입니다.");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", message, fieldErrors));
  }

  /**
   * 역직렬화 실패. UUID·숫자·enum 형식이 틀리면 Bean Validation까지 가지 못하고 여기로 온다.
   *
   * <p>필드명을 버리면 프론트가 어느 값을 고쳐야 하는지 알 수 없다. Jackson이 남긴 경로에서 필드명을 되살린다.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(
      HttpMessageNotReadableException exception) {
    String field = fieldOf(exception.getCause());
    if (field == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ErrorResponse("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다."));
    }
    String detail = typeMessage(exception.getCause());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "INVALID_REQUEST",
                field + ": " + detail,
                List.of(new ErrorResponse.FieldError(field, detail))));
  }

  private String fieldOf(Throwable cause) {
    if (!(cause instanceof MismatchedInputException mismatch)) {
      return null;
    }
    return mismatch.getPath().stream()
        .map(JsonMappingException.Reference::getFieldName)
        .filter(Objects::nonNull)
        .reduce((first, second) -> second)
        .orElse(null);
  }

  private String typeMessage(Throwable cause) {
    if (!(cause instanceof InvalidFormatException invalidFormat)) {
      return "형식이 올바르지 않습니다.";
    }
    if (UUID.class.equals(invalidFormat.getTargetType())) {
      return "UUID 형식이 아닙니다.";
    }
    return "형식이 올바르지 않습니다.";
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParam(
      MissingServletRequestParameterException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                "INVALID_REQUEST", exception.getParameterName() + ": 필수 파라미터가 누락됐습니다."));
  }
}
