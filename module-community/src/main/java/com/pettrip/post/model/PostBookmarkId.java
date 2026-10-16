package com.pettrip.post.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PostBookmarkId implements Serializable {

  private UUID userId;
  private UUID postId;

  public PostBookmarkId() {}

  public PostBookmarkId(UUID userId, UUID postId) {
    this.userId = userId;
    this.postId = postId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PostBookmarkId that)) {
      return false;
    }
    return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, postId);
  }
}
