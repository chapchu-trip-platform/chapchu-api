package com.pettrip.review.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "review_recommendations")
@IdClass(ReviewRecommendationId.class)
@EntityListeners(AuditingEntityListener.class)
public class ReviewRecommendation {

  @Id
  @Column(name = "review_id")
  private UUID reviewId;

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected ReviewRecommendation() {}

  public ReviewRecommendation(UUID reviewId, UUID userId) {
    this.reviewId = reviewId;
    this.userId = userId;
  }

  public UUID getReviewId() {
    return reviewId;
  }

  public UUID getUserId() {
    return userId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
