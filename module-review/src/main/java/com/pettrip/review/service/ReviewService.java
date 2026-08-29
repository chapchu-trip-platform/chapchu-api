package com.pettrip.review.service;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.review.controller.ReviewCreateRequest;
import com.pettrip.review.model.Review;
import com.pettrip.review.model.ReviewRecommendation;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

  private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

  private final ReviewRepository reviewRepository;
  private final ReviewRecommendationRepository reviewRecommendationRepository;
  private final ReviewEmbeddingService reviewEmbeddingService;
  private final PetRepository petRepository;

  public ReviewService(
      ReviewRepository reviewRepository,
      ReviewRecommendationRepository reviewRecommendationRepository,
      ReviewEmbeddingService reviewEmbeddingService,
      PetRepository petRepository) {
    this.reviewRepository = reviewRepository;
    this.reviewRecommendationRepository = reviewRecommendationRepository;
    this.reviewEmbeddingService = reviewEmbeddingService;
    this.petRepository = petRepository;
  }

  public Review createReview(UUID userId, ReviewCreateRequest request) {
    if (!petRepository.existsByIdAndUserId(request.petId(), userId)) {
      throw new PetNotOwnedException();
    }
    Review review =
        new Review(
            request.placeId(),
            userId,
            request.petId(),
            request.rating(),
            request.contents(),
            request.weather());
    review.setCoursePlaceId(request.coursePlaceId());
    Review saved = reviewRepository.save(review);
    log.info(
        "[리뷰] 작성 완료 — reviewId={}, placeId={}, petId={}, rating={}",
        saved.getId(),
        saved.getPlaceId(),
        saved.getPetId(),
        saved.getRating());
    reviewEmbeddingService.generateAndSave(saved);
    return saved;
  }

  @Transactional
  public void deleteReview(UUID userId, UUID reviewId) {
    Review review = findReview(reviewId);
    if (!userId.equals(review.getUserId())) {
      throw new ReviewNotOwnerException();
    }
    reviewRepository.delete(review);
    log.info("[리뷰] 삭제 완료 — reviewId={}", reviewId);
  }

  public List<Review> listMyReviews(UUID userId) {
    return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public List<Review> listPlaceReviews(String placeId) {
    return reviewRepository.findByPlaceIdOrderByCreatedAtDesc(placeId);
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
