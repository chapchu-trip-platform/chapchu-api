package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 목록 카드용 요약. 본문·참조 등 상세는 {@code GET /posts/{id}}에서 가져온다.
 *
 * @param thumbnail 대표 사진(첫 장). 사진 없는 글이면 null
 * @param photoCount 첨부된 사진 수. 목록에서 "+N" 표시에 쓴다
 */
public record PostSummaryResponse(
    UUID id,
    String title,
    String nickname,
    int recommendationCount,
    int commentCount,
    int photoCount,
    PostResponse.PhotoView thumbnail,
    LocalDateTime createdAt) {}
