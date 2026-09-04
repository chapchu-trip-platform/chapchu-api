package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param recommended 요청한 사용자가 이 글을 추천했는지. 추천 취소 버튼을 그리려면 필요하다
 * @param bookmarked 요청한 사용자가 이 글을 북마크했는지
 */
public record PostResponse(
    UUID id,
    UUID petId,
    UUID photoId,
    UUID courseId,
    String title,
    String content,
    int viewCount,
    int recommendationCount,
    int commentCount,
    boolean recommended,
    boolean bookmarked,
    String nickname,
    String photoUrl,
    LocalDateTime createdAt) {}
