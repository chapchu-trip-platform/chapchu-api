package com.pettrip.review.service;

import com.pettrip.review.model.Review;
import com.pettrip.review.model.ReviewRecommendation;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ReviewRecommendationRepository reviewRecommendationRepository;

  public ReviewService(
      ReviewRepository reviewRepository,
      ReviewRecommendationRepository reviewRecommendationRepository) {
    this.reviewRepository = reviewRepository;
    this.reviewRecommendationRepository = reviewRecommendationRepository;
  }

  public List<Review> listMyReviews(UUID userId) {
    return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Transactional
  public void recommend(UUID userId, UUID reviewId) {
    Review review = findReview(reviewId);
    if (reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId)) {
      throw new ReviewAlreadyRecommendedException();
    }
    reviewRecommendationRepository.save(new ReviewRecommendation(reviewId, userId));
    review.incrementRecommendationCount();
    reviewRepository.save(review);
  }

  @Transactional
  public void cancelRecommendation(UUID userId, UUID reviewId) {
    Review review = findReview(reviewId);
    if (!reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId)) {
      throw new ReviewRecommendationNotFoundException();
    }
    reviewRecommendationRepository.deleteByReviewIdAndUserId(reviewId, userId);
    review.decrementRecommendationCount();
    reviewRepository.save(review);
  }

  private Review findReview(UUID reviewId) {
    return reviewRepository.findById(reviewId).orElseThrow(ReviewNotFoundException::new);
  }
}
