package com.pettrip.post.controller;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * @param title 선택. 컬럼이 VARCHAR(100)이라 길이를 넘기면 DB에서 터지므로 여기서 막는다
 * @param content 선택. 컬럼이 TEXT라 길이 제한이 없다
 */
public record PostCreateRequest(
    @NotNull UUID petId,
    @NotNull UUID photoId,
    @NotNull UUID courseId,
    @Size(max = 100) String title,
    String content) {}
