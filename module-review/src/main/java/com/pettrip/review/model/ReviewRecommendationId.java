package com.pettrip.review.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ReviewRecommendationId implements Serializable {

  private UUID reviewId;
  private UUID userId;

  public ReviewRecommendationId() {}

  public ReviewRecommendationId(UUID reviewId, UUID userId) {
    this.reviewId = reviewId;
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ReviewRecommendationId that)) {
      return false;
    }
    return Objects.equals(reviewId, that.reviewId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reviewId, userId);
  }
}
