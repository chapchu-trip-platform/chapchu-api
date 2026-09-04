package com.pettrip.post.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.post.service.PostService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
public class PostController {

  private final PostService postService;

  public PostController(PostService postService) {
    this.postService = postService;
  }

  @GetMapping
  public PostListResponse listPosts(
      @RequestParam(name = "sort", defaultValue = "latest") String sort,
      @RequestParam(name = "cursor", required = false) String cursor,
      @RequestParam(name = "size", defaultValue = "20") int size) {
    return postService.listPosts(sort, cursor, size);
  }

  @GetMapping("/{postId}")
  public PostResponse getPost(@PathVariable UUID postId) {
    return postService.getPost(postId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostResponse createPost(
      @CurrentUserId UUID userId, @RequestBody @Valid PostCreateRequest request) {
    return postService.createPost(
        userId,
        request.petId(),
        request.photoId(),
        request.courseId(),
        request.title(),
        request.content());
  }

  @PatchMapping("/{postId}")
  public PostResponse updatePost(
      @CurrentUserId UUID userId,
      @PathVariable UUID postId,
      @RequestBody @Valid PostUpdateRequest request) {
    return postService.updatePost(userId, postId, request.title(), request.content());
  }

  @DeleteMapping("/{postId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePost(@CurrentUserId UUID userId, @PathVariable UUID postId) {
    postService.deletePost(userId, postId);
  }
}
