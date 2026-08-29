package com.pettrip.review.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ReviewResponse create(
      @CurrentUserId UUID userId, @RequestBody @Valid ReviewCreateRequest request) {
    return ReviewResponse.from(reviewService.createReview(userId, request));
  }

  @DeleteMapping("/{reviewId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@CurrentUserId UUID userId, @PathVariable UUID reviewId) {
    reviewService.deleteReview(userId, reviewId);
  }
}
