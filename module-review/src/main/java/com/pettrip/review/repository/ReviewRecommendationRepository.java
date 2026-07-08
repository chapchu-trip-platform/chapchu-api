package com.pettrip.review.repository;

import com.pettrip.review.model.ReviewRecommendation;
import com.pettrip.review.model.ReviewRecommendationId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecommendationRepository
    extends JpaRepository<ReviewRecommendation, ReviewRecommendationId> {

  boolean existsByReviewIdAndUserId(UUID reviewId, UUID userId);

  void deleteByReviewIdAndUserId(UUID reviewId, UUID userId);
}
