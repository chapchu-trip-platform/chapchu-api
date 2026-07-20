package com.pettrip.post.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PostReportId implements Serializable {

  private UUID postId;
  private UUID userId;

  public PostReportId() {}

  public PostReportId(UUID postId, UUID userId) {
    this.postId = postId;
    this.userId = userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PostReportId that)) {
      return false;
    }
    return Objects.equals(postId, that.postId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postId, userId);
  }
}
