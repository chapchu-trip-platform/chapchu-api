package com.pettrip.review.controller;

import com.pettrip.review.model.Review;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
    UUID id,
    String placeId,
    UUID petId,
    Short rating,
    String contents,
    String weather,
    int recommendationCount,
    LocalDateTime createdAt,
    UUID coursePlaceId) {

  public static ReviewResponse from(Review review) {
    return new ReviewResponse(
        review.getId(),
        review.getPlaceId(),
        review.getPetId(),
        review.getRating(),
        review.getContents(),
        review.getWeather(),
        review.getRecommendationCount(),
        review.getCreatedAt(),
        review.getCoursePlaceId());
  }
}
