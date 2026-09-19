package com.pettrip.post.controller;

import com.pettrip.post.model.PostType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 목록 카드용 요약. 본문·참조 등 상세는 {@code GET /posts/{id}}에서 가져온다.
 *
 * @param thumbnail 대표 사진(첫 장). 사진 없는 글이면 null
 * @param authorProfilePhotoUrl 작성자 프로필 사진 presigned URL(10분). 없으면 null
 * @param photoCount 첨부된 사진 수. 목록에서 "+N" 표시에 쓴다
 * @param postType 글 종류 (GENERAL=일반, TRAVEL_REVIEW=여행후기)
 */
public record PostSummaryResponse(
    UUID id,
    PostType postType,
    String title,
    String nickname,
    String authorProfilePhotoUrl,
    int recommendationCount,
    int commentCount,
    int photoCount,
    PostResponse.PhotoView thumbnail,
    LocalDateTime createdAt) {}
