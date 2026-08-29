package com.pettrip.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.review.controller.ReviewCreateRequest;
import com.pettrip.review.model.Review;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReviewRecommendationRepository reviewRecommendationRepository;
  @Mock private ReviewEmbeddingService reviewEmbeddingService;
  @Mock private PetRepository petRepository;

  @InjectMocks private ReviewService reviewService;

  @Test
  void 리뷰_생성시_리뷰와_임베딩이_저장된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest("place-1", petId, (short) 4, "좋았어요", "SUNNY", null);
    Review saved = new Review("place-1", userId, petId, (short) 4, "좋았어요", "SUNNY");
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    Review result = reviewService.createReview(userId, request);

    assertThat(result.getPlaceId()).isEqualTo("place-1");
    assertThat(result.getWeather()).isEqualTo("SUNNY");
    verify(reviewEmbeddingService).generateAndSave(saved);
  }

  @Test
  void 코스_방문지_연결해서_리뷰_생성된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest("place-1", petId, (short) 5, "코스 최고!", "SUNNY", coursePlaceId);
    Review saved = new Review("place-1", userId, petId, (short) 5, "코스 최고!", "SUNNY");
    saved.setCoursePlaceId(coursePlaceId);
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    Review result = reviewService.createReview(userId, request);

    assertThat(result.getCoursePlaceId()).isEqualTo(coursePlaceId);
  }

  @Test
  void 날씨없이_리뷰_생성시_정상_저장된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest("place-2", petId, (short) 3, "그냥 그랬어요", null, null);
    Review saved = new Review("place-2", userId, petId, (short) 3, "그냥 그랬어요", null);
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    Review result = reviewService.createReview(userId, request);

    assertThat(result.getWeather()).isNull();
    verify(reviewEmbeddingService).generateAndSave(saved);
  }

  @Test
  void 타인_반려동물로_리뷰_생성시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID otherPetId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest("place-1", otherPetId, (short) 4, "좋았어요", null, null);
    when(petRepository.existsByIdAndUserId(otherPetId, userId)).thenReturn(false);

    assertThatThrownBy(() -> reviewService.createReview(userId, request))
        .isInstanceOf(PetNotOwnedException.class);

    verify(reviewRepository, never()).save(any());
    verify(reviewEmbeddingService, never()).generateAndSave(any());
  }

  @Test
  void 본인리뷰_삭제시_삭제된다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", userId, UUID.randomUUID(), (short) 5, "완벽해요", null);
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    reviewService.deleteReview(userId, reviewId);

    verify(reviewRepository).delete(review);
  }

  @Test
  void 타인리뷰_삭제시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review = new Review("place-1", otherUserId, UUID.randomUUID(), (short) 5, "내 리뷰", null);
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.deleteReview(userId, reviewId))
        .isInstanceOf(ReviewNotOwnerException.class);

    verify(reviewRepository, never()).delete(any());
  }

  @Test
  void 장소별_리뷰_목록이_최신순으로_반환된다() {
    String placeId = "place-1";
    List<Review> expected =
        List.of(
            new Review(placeId, UUID.randomUUID(), UUID.randomUUID(), (short) 4, "리뷰1", "RAINY"),
            new Review(placeId, UUID.randomUUID(), UUID.randomUUID(), (short) 3, "리뷰2", null));
    when(reviewRepository.findByPlaceIdOrderByCreatedAtDesc(placeId)).thenReturn(expected);

    List<Review> result = reviewService.listPlaceReviews(placeId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getContents()).isEqualTo("리뷰1");
  }

  @Test
  void recommend는_추천을_저장하고_카운트를_증가시킨다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review =
        new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요", null);
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewRecommendationRepository.existsByReviewIdAndUserId(reviewId, userId))
        .thenReturn(false);

    reviewService.recommend(userId, reviewId);

    assertThat(review.getRecommendationCount()).isEqualTo(1);
    verify(reviewRecommendationRepository).save(any());
    verify(reviewRepository).save(review);
  }

  @Test
  void cancelRecommendation는_추천을_삭제하고_카운트를_감소시킨다() {
    UUID userId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    Review review =
        new Review("place-1", UUID.randomUUID(), UUID.randomUUID(), (short) 5, "좋아요", null);
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
