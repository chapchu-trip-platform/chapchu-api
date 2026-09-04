package com.pettrip.comment.controller;

import com.pettrip.comment.service.CommentService;
import com.pettrip.common.service.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping("/posts/{postId}/comments")
  public List<CommentResponse> listComments(@PathVariable UUID postId) {
    return commentService.listComments(postId);
  }

  @PostMapping("/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse createComment(
      @CurrentUserId UUID userId,
      @PathVariable UUID postId,
      @RequestBody @Valid CommentCreateRequest request) {
    return commentService.createComment(
        userId, postId, request.parentCommentId(), request.content());
  }

  @PatchMapping("/comments/{commentId}")
  public CommentResponse updateComment(
      @CurrentUserId UUID userId,
      @PathVariable UUID commentId,
      @RequestBody @Valid CommentUpdateRequest request) {
    return commentService.updateComment(userId, commentId, request.content());
  }

  @DeleteMapping("/comments/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(@CurrentUserId UUID userId, @PathVariable UUID commentId) {
    commentService.deleteComment(userId, commentId);
  }
}
