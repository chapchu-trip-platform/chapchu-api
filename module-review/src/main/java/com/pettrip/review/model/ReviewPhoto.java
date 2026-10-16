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

/** 리뷰-사진 연결. photoId는 UUID 값으로만 보관하고 Photo 엔티티에 JPA 연관을 걸지 않는다(모듈 경계). */
@Entity
@Table(name = "review_photos")
@EntityListeners(AuditingEntityListener.class)
public class ReviewPhoto {

  @Id
  @Column(name = "review_photo_id", columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id = UuidCreator.getTimeOrderedEpoch();

  @Column(name = "review_id", nullable = false)
  private UUID reviewId;

  @Column(name = "photo_id", nullable = false)
  private UUID photoId;

  @Column(name = "photo_order", nullable = false)
  private short photoOrder;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected ReviewPhoto() {}

  public ReviewPhoto(UUID reviewId, UUID photoId, short photoOrder) {
    this.reviewId = reviewId;
    this.photoId = photoId;
    this.photoOrder = photoOrder;
  }

  public UUID getId() {
    return id;
  }

  public UUID getReviewId() {
    return reviewId;
  }

  public UUID getPhotoId() {
    return photoId;
  }

  public short getPhotoOrder() {
    return photoOrder;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
