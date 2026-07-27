package com.pettrip.review.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.review.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/reviews")
public class MyReviewController {

  private final ReviewService reviewService;

  public MyReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping
  public List<ReviewResponse> listMyReviews() {
    return reviewService.listMyReviews(TempAuthContext.TEMP_USER_ID).stream()
        .map(ReviewResponse::from)
        .toList();
  }
}
