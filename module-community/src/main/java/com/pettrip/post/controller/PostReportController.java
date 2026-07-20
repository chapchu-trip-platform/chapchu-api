package com.pettrip.post.controller;

import com.pettrip.common.service.TempAuthContext;
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
  public void report(@PathVariable UUID postId, @RequestBody @Valid PostReportRequest request) {
    postService.report(
        TempAuthContext.TEMP_USER_ID, postId, request.reportReason(), request.reportDetail());
  }
}
