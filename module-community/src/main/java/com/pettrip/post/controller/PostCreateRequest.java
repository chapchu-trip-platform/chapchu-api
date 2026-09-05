package com.pettrip.post.controller;

import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * posts는 자유게시판이라 참조가 모두 선택이다. 값을 보낸 참조만 본인 것인지 확인한다.
 *
 * @param petId 선택. 함께한 반려동물을 붙일 때만 보낸다
 * @param photoId 선택. 사진 없이 글만 쓸 수 있다
 * @param courseId 선택. 다녀온 여행 코스를 붙일 때만 보낸다
 * @param title 선택. 컬럼이 VARCHAR(100)이라 길이를 넘기면 DB에서 터지므로 여기서 막는다
 * @param content 선택. 컬럼이 TEXT라 길이 제한이 없다
 */
public record PostCreateRequest(
    UUID petId, UUID photoId, UUID courseId, @Size(max = 100) String title, String content) {}
