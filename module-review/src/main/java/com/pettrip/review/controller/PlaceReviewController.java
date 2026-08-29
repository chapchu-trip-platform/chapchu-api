package com.pettrip.review.controller;

import com.pettrip.review.service.ReviewService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlaceReviewController {

  private final ReviewService reviewService;

  public PlaceReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping("/places/{placeId}/reviews")
  public List<ReviewResponse> listPlaceReviews(@PathVariable String placeId) {
    return reviewService.listPlaceReviews(placeId).stream().map(ReviewResponse::from).toList();
  }
}
