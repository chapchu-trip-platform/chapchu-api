package com.pettrip.review.model;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
public class Review {

  @Id
  @Column(name = "review_id", columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id = UuidCreator.getTimeOrderedEpoch();

  @Column(name = "place_id", nullable = false)
  private String placeId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "pet_id", nullable = false)
  private UUID petId;

  @Column(name = "rating")
  private Short rating;

  @Column(name = "contents")
  private String contents;

  @Column(name = "recommendation_count")
  private int recommendationCount = 0;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected Review() {}

  public Review(String placeId, UUID userId, UUID petId, Short rating, String contents) {
    this.placeId = placeId;
    this.userId = userId;
    this.petId = petId;
    this.rating = rating;
    this.contents = contents;
  }

  public void incrementRecommendationCount() {
    this.recommendationCount++;
  }

  public void decrementRecommendationCount() {
    this.recommendationCount = Math.max(0, this.recommendationCount - 1);
  }

  public UUID getId() {
    return id;
  }

  public String getPlaceId() {
    return placeId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getPetId() {
    return petId;
  }

  public Short getRating() {
    return rating;
  }

  public String getContents() {
    return contents;
  }

  public int getRecommendationCount() {
    return recommendationCount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
