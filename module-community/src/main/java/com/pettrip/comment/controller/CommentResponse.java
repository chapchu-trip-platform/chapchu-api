package com.pettrip.comment.controller;

import com.pettrip.comment.model.Comment;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID postId,
    UUID parentCommentId,
    int depth,
    int commentOrder,
    String content,
    LocalDateTime createdAt) {

  public static CommentResponse from(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getPostId(),
        comment.getParentCommentId(),
        comment.getDepth(),
        comment.getCommentOrder(),
        comment.getContent(),
        comment.getCreatedAt());
  }
}
