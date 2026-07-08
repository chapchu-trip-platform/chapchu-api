package com.pettrip.post.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.post.model.Post;
import com.pettrip.post.service.PostService;
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
import org.springframework.web.bind.annotation.RequestMapping;
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
  public List<PostResponse> listPosts() {
    return postService.listPosts().stream().map(PostResponse::from).toList();
  }

  @GetMapping("/{postId}")
  public PostResponse getPost(@PathVariable UUID postId) {
    return PostResponse.from(postService.getPost(postId));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostResponse createPost(@RequestBody @Valid PostCreateRequest request) {
    Post post =
        postService.createPost(
            TempAuthContext.TEMP_USER_ID,
            request.petId(),
            request.photoId(),
            request.courseId(),
            request.title(),
            request.content());
    return PostResponse.from(post);
  }

  @PatchMapping("/{postId}")
  public PostResponse updatePost(
      @PathVariable UUID postId, @RequestBody PostUpdateRequest request) {
    Post post =
        postService.updatePost(
            TempAuthContext.TEMP_USER_ID, postId, request.title(), request.content());
    return PostResponse.from(post);
  }

  @DeleteMapping("/{postId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePost(@PathVariable UUID postId) {
    postService.deletePost(TempAuthContext.TEMP_USER_ID, postId);
  }
}
