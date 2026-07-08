package com.pettrip.comment.model;

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
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
public class Comment {

  @Id
  @Column(name = "comment_id", columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id = UuidCreator.getTimeOrderedEpoch();

  @Column(name = "post_id", nullable = false)
  private UUID postId;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "parent_comment_id")
  private UUID parentCommentId;

  @Column(name = "depth")
  private int depth;

  @Column(name = "comment_order")
  private int commentOrder;

  @Column(name = "content")
  private String content;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  protected Comment() {}

  public Comment(
      UUID postId, UUID userId, UUID parentCommentId, int depth, int commentOrder, String content) {
    this.postId = postId;
    this.userId = userId;
    this.parentCommentId = parentCommentId;
    this.depth = depth;
    this.commentOrder = commentOrder;
    this.content = content;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPostId() {
    return postId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getParentCommentId() {
    return parentCommentId;
  }

  public int getDepth() {
    return depth;
  }

  public int getCommentOrder() {
    return commentOrder;
  }

  public String getContent() {
    return content;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
