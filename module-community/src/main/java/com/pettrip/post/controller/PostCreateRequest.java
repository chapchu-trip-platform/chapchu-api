package com.pettrip.post.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * posts는 자유게시판이라 참조가 모두 선택이다. 값을 보낸 참조만 본인 것인지 확인한다.
 *
 * @param petId 선택. 함께한 반려동물을 붙일 때만 보낸다
 * @param courseId 선택. 다녀온 여행 코스를 붙일 때만 보낸다
 * @param title 선택. 컬럼이 VARCHAR(100)이라 길이를 넘기면 DB에서 터지므로 여기서 막는다
 * @param content 선택. 컬럼이 TEXT라 길이 제한이 없다
 * @param photos 선택. 사진 없이 글만 쓸 수 있다. 최대 10장. photoKey만 보내면 서버가 photo를 만든다
 */
public record PostCreateRequest(
    UUID petId,
    UUID courseId,
    @Size(max = 100) String title,
    String content,
    @Size(max = 10) @Valid List<PhotoEntry> photos) {

  /** 첨부 사진 1장. photoKey는 upload-url로 발급받은 S3 경로. photoId는 서버가 생성한다. */
  public record PhotoEntry(@NotBlank String photoKey, LocalDate takenAt) {}
}
