package com.pettrip.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.review.model.Review;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReviewRecommendationRepository reviewRecommendationRepository;

  private ReviewService reviewService;

  @BeforeEach
  void setUp() {
    reviewService = new ReviewService(reviewRepository, reviewRecommendationRepository);
  }

  @Test
  void listMyReviews는_레포지토리에_위임한다() {
    UUID userId = UUID.randomUUID();
    when(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

    reviewService.listMyReviews(userId);

    verify(reviewRepository).findByUserIdOrderByCreatedAtDesc(userId);
  }

  @Test
  void recommend는_리뷰가_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.recommend(userId, reviewId))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  void recommend는_이미_추천했으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요");
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId))
        .thenReturn(true);

    assertThatThrownBy(() -> reviewService.recommend(userId, reviewId))
        .isInstanceOf(ReviewAlreadyRecommendedException.class);
  }

  @Test
  void recommend는_추천을_저장하고_카운트를_증가시킨다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요");
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId))
        .thenReturn(false);

    reviewService.recommend(userId, reviewId);

    assertThat(review.getRecommendationCount()).isEqualTo(1);
    verify(reviewRecommendationRepository).save(any());
    verify(reviewRepository).save(review);
  }

  @Test
  void cancelRecommendation는_추천한_적이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요");
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId))
        .thenReturn(false);

    assertThatThrownBy(() -> reviewService.cancelRecommendation(userId, reviewId))
        .isInstanceOf(ReviewRecommendationNotFoundException.class);

    verify(reviewRecommendationRepository, never()).deleteByReviewIdAndUserId(any(), any());
  }

  @Test
  void cancelRecommendation는_추천을_삭제하고_카운트를_감소시킨다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요");
    review.incrementRecommendationCount();
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId))
        .thenReturn(true);

    reviewService.cancelRecommendation(userId, reviewId);

    assertThat(review.getRecommendationCount()).isEqualTo(0);
    verify(reviewRecommendationRepository, times(1)).deleteByReviewIdAndUserId(reviewId, userId);
    verify(reviewRepository).save(review);
  }
}
