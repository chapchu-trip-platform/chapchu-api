package com.pettrip.post.controller;

import java.time.LocalDateTime;
import java.util.UUID;

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
    String nickname,
    String photoUrl,
    LocalDateTime createdAt) {}
