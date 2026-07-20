package com.pettrip.post.repository;

import com.pettrip.post.model.PostRecommendation;
import com.pettrip.post.model.PostRecommendationId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRecommendationRepository
    extends JpaRepository<PostRecommendation, PostRecommendationId> {

  boolean existsByPostIdAndUserId(UUID postId, UUID userId);

  void deleteByPostIdAndUserId(UUID postId, UUID userId);
}
