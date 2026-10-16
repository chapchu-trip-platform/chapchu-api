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
@Table(name = "post_bookmarks")
@IdClass(PostBookmarkId.class)
@EntityListeners(AuditingEntityListener.class)
public class PostBookmark {

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Id
  @Column(name = "post_id")
  private UUID postId;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected PostBookmark() {}

  public PostBookmark(UUID userId, UUID postId) {
    this.userId = userId;
    this.postId = postId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getPostId() {
    return postId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
