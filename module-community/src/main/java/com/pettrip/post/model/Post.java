package com.pettrip.post.model;

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
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
public class Post {

  @Id
  @Column(name = "post_id", columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id = UuidCreator.getTimeOrderedEpoch();

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "pet_id", nullable = false)
  private UUID petId;

  @Column(name = "photo_id", nullable = false)
  private UUID photoId;

  @Column(name = "course_id", nullable = false)
  private UUID courseId;

  @Column(name = "title", length = 100)
  private String title;

  @Column(name = "content")
  private String content;

  @Column(name = "view_count")
  private int viewCount = 0;

  @Column(name = "recommendation_count")
  private int recommendationCount = 0;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected Post() {}

  public Post(UUID userId, UUID petId, UUID photoId, UUID courseId, String title, String content) {
    this.userId = userId;
    this.petId = petId;
    this.photoId = photoId;
    this.courseId = courseId;
    this.title = title;
    this.content = content;
  }

  public void update(String newTitle, String newContent) {
    if (newTitle != null) {
      this.title = newTitle;
    }
    if (newContent != null) {
      this.content = newContent;
    }
  }

  public void increaseViewCount() {
    this.viewCount++;
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

  public UUID getUserId() {
    return userId;
  }

  public UUID getPetId() {
    return petId;
  }

  public UUID getPhotoId() {
    return photoId;
  }

  public UUID getCourseId() {
    return courseId;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public int getViewCount() {
    return viewCount;
  }

  public int getRecommendationCount() {
    return recommendationCount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
