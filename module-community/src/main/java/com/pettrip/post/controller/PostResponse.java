package com.pettrip.post.controller;

import com.pettrip.post.model.Post;
import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
    UUID id,
    UUID petId,
    UUID photoId,
    UUID courseId,
    String title,
    String content,
    int viewCount,
    int recommendationCount,
    LocalDateTime createdAt) {

  public static PostResponse from(Post post) {
    return new PostResponse(
        post.getId(),
        post.getPetId(),
        post.getPhotoId(),
        post.getCourseId(),
        post.getTitle(),
        post.getContent(),
        post.getViewCount(),
        post.getRecommendationCount(),
        post.getCreatedAt());
  }
}
