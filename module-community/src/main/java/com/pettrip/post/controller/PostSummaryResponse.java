package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 목록 카드용 요약. 본문·참조 등 상세는 {@code GET /posts/{id}}에서 가져온다.
 *
 * @param thumbnail 대표 사진(첫 장). 사진 없는 글이면 null
 * @param authorProfilePhotoUrl 작성자 프로필 사진 presigned URL(10분). 없으면 null
 */
public record PostSummaryResponse(
    UUID id,
    String title,
    String nickname,
    String authorProfilePhotoUrl,
    int recommendationCount,
    int commentCount,
    PostResponse.PhotoView thumbnail,
    LocalDateTime createdAt) {}
