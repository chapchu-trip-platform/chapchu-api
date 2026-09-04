package com.pettrip.comment.controller;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param content 삭제된 댓글이면 "삭제된 댓글입니다"
 * @param nickname 작성자 닉네임. 삭제된 댓글이면 null
 * @param deleted 삭제된 댓글인지. 대댓글이 달려 있어도 스레드를 유지하려고 행을 남긴다
 */
public record CommentResponse(
    UUID id,
    UUID postId,
    UUID parentCommentId,
    int depth,
    int commentOrder,
    String content,
    String nickname,
    boolean deleted,
    LocalDateTime createdAt) {}
