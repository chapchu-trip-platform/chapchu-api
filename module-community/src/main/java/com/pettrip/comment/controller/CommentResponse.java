package com.pettrip.comment.controller;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID postId,
    UUID parentCommentId,
    int depth,
    int commentOrder,
    String content,
    String nickname,
    LocalDateTime createdAt) {}
