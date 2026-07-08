package com.pettrip.review.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.review.service.ReviewService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews/{reviewId}/recommendations")
public class ReviewRecommendationController {

  private final ReviewService reviewService;

  public ReviewRecommendationController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void recommend(@PathVariable UUID reviewId) {
    reviewService.recommend(TempAuthContext.TEMP_USER_ID, reviewId);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelRecommendation(@PathVariable UUID reviewId) {
    reviewService.cancelRecommendation(TempAuthContext.TEMP_USER_ID, reviewId);
  }
}
