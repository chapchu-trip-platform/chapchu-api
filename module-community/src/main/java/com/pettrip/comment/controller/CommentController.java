package com.pettrip.comment.controller;

import com.pettrip.comment.model.Comment;
import com.pettrip.comment.service.CommentService;
import com.pettrip.common.service.TempAuthContext;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  @PostMapping("/posts/{postId}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse createComment(
      @PathVariable UUID postId, @RequestBody @Valid CommentCreateRequest request) {
    Comment comment =
        commentService.createComment(
            TempAuthContext.TEMP_USER_ID, postId, request.parentCommentId(), request.content());
    return CommentResponse.from(comment);
  }

  @DeleteMapping("/comments/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteComment(@PathVariable UUID commentId) {
    commentService.deleteComment(TempAuthContext.TEMP_USER_ID, commentId);
  }
}
