package com.pettrip.post.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.post.service.PostService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts/{postId}/bookmarks")
public class PostBookmarkController {

  private final PostService postService;

  public PostBookmarkController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void bookmark(@CurrentUserId UUID userId, @PathVariable UUID postId) {
    postService.bookmark(userId, postId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelBookmark(@CurrentUserId UUID userId, @PathVariable UUID postId) {
    postService.cancelBookmark(userId, postId);
  }
}
