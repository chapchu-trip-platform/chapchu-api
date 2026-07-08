package com.pettrip.post.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "post_reports")
@IdClass(PostReportId.class)
@EntityListeners(AuditingEntityListener.class)
public class PostReport {

  @Id
  @Column(name = "post_id")
  private UUID postId;

  @Id
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "report_reason", length = 50)
  private String reportReason;

  @Column(name = "report_detail")
  private String reportDetail;

  @Enumerated(EnumType.STRING)
  @Column(name = "report_status", length = 20)
  private ReportStatus reportStatus;

  @CreatedDate
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected PostReport() {}

  public PostReport(UUID postId, UUID userId, String reportReason, String reportDetail) {
    this.postId = postId;
    this.userId = userId;
    this.reportReason = reportReason;
    this.reportDetail = reportDetail;
    this.reportStatus = ReportStatus.PENDING;
  }

  public UUID getPostId() {
    return postId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getReportReason() {
    return reportReason;
  }

  public String getReportDetail() {
    return reportDetail;
  }

  public ReportStatus getReportStatus() {
    return reportStatus;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
