package com.pettrip.post.model;

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
@Table(name = "post_recommendations")
@IdClass(PostRecommendationId.class)
@EntityListeners(AuditingEntityListener.class)
public class PostRecommendation {

  @Id
  @Column(name = "post_id")
  private UUID postId;

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected PostRecommendation() {}

  public PostRecommendation(UUID postId, UUID userId) {
    this.postId = postId;
    this.userId = userId;
  }

  public UUID getPostId() {
    return postId;
  }

  public UUID getUserId() {
    return userId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
