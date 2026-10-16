package com.pettrip.review.controller;

import com.pettrip.review.model.Review;
import com.pettrip.review.service.ReviewDetail;
import com.pettrip.review.service.ReviewPhotoView;
import java.time.LocalDateTime;
import java.util.List;
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
    UUID coursePlaceId,
    List<ReviewPhotoView> photos) {

  public static ReviewResponse of(ReviewDetail detail) {
    Review review = detail.review();
    return new ReviewResponse(
        review.getId(),
        review.getPlaceId(),
        review.getPetId(),
        review.getRating(),
        review.getContents(),
        review.getWeather(),
        review.getRecommendationCount(),
        review.getCreatedAt(),
        review.getCoursePlaceId(),
        detail.photos());
  }
}
