package com.pettrip.post.controller;

import com.pettrip.common.service.TempAuthContext;
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
@RequestMapping("/posts/{postId}/recommendations")
public class PostRecommendationController {

  private final PostService postService;

  public PostRecommendationController(PostService postService) {
    this.postService = postService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void recommend(@PathVariable UUID postId) {
    postService.recommend(TempAuthContext.TEMP_USER_ID, postId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelRecommendation(@PathVariable UUID postId) {
    postService.cancelRecommendation(TempAuthContext.TEMP_USER_ID, postId);
  }
}
