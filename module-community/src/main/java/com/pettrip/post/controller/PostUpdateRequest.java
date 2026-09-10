package com.pettrip.post.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * null인 필드는 그대로 둔다. {@link PostCreateRequest}와 같은 이유로 title 길이를 제한한다.
 *
 * @param photos 사진 목록을 통째로 바꾼다. null이면 사진을 손대지 않고, 빈 배열이면 전부 뗀다. 남길 사진도 함께 보내야 한다
 */
public record PostUpdateRequest(
    @Size(max = 100) String title,
    String content,
    @Size(max = 10) @Valid List<PostCreateRequest.PhotoEntry> photos) {}
