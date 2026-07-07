package com.pettrip.photo.model;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "photos")
@EntityListeners(AuditingEntityListener.class)
public class Photo {

  @Id
  @Column(name = "photo_id", columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id = UuidCreator.getTimeOrderedEpoch();

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "course_place_id", nullable = false)
  private UUID coursePlaceId;

  @Column(name = "photo_url", length = 500)
  private String photoUrl;

  @Column(name = "taken_at")
  private LocalDate takenAt;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected Photo() {}

  public Photo(UUID userId, UUID coursePlaceId, String photoUrl, LocalDate takenAt) {
    this.userId = userId;
    this.coursePlaceId = coursePlaceId;
    this.photoUrl = photoUrl;
    this.takenAt = takenAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getCoursePlaceId() {
    return coursePlaceId;
  }

  public String getPhotoUrl() {
    return photoUrl;
  }

  public LocalDate getTakenAt() {
    return takenAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
