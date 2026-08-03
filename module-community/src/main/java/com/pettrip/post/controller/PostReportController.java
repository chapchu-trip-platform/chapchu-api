package com.pettrip.post.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.post.service.PostService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/{postId}/reports")
public class PostReportController {

  private final PostService postService;

  public PostReportController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void report(
      @CurrentUserId UUID userId,
      @PathVariable UUID postId,
      @RequestBody @Valid PostReportRequest request) {
    postService.report(userId, postId, request.reportReason(), request.reportDetail());
  }
}
