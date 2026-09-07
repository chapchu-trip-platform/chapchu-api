package com.pettrip.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.repository.PhotoRepository;
import com.pettrip.photo.service.PhotoNotFoundException;
import com.pettrip.photo.service.PhotoService;
import com.pettrip.review.controller.ReviewCreateRequest;
import com.pettrip.review.model.Review;
import com.pettrip.review.model.ReviewPhoto;
import com.pettrip.review.repository.ReviewPhotoRepository;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReviewServiceTest {

  @Mock private ReviewRepository reviewRepository;
  @Mock private ReviewRecommendationRepository reviewRecommendationRepository;
  @Mock private ReviewEmbeddingService reviewEmbeddingService;
  @Mock private PetRepository petRepository;
  @Mock private ReviewPhotoRepository reviewPhotoRepository;
  @Mock private PhotoRepository photoRepository;
  @Mock private PhotoService photoService;

  @InjectMocks private ReviewService reviewService;

  @BeforeEach
  void setUp() {
    when(reviewPhotoRepository.findByReviewIdInOrderByPhotoOrderAsc(any())).thenReturn(List.of());
    when(photoRepository.findAllById(any())).thenReturn(List.of());
  }

  private ReviewCreateRequest request(
      String placeId,
      UUID petId,
      short rating,
      String contents,
      String weather,
      UUID coursePlaceId) {
    return new ReviewCreateRequest(placeId, petId, rating, contents, weather, coursePlaceId, null);
  }

  @Test
  void 리뷰_생성시_리뷰와_임베딩이_저장된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    ReviewCreateRequest request = request("place-1", petId, (short) 4, "좋았어요", "SUNNY", null);
    Review saved = new Review("place-1", userId, petId, (short) 4, "좋았어요", "SUNNY");
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    ReviewDetail result = reviewService.createReview(userId, request);

    assertThat(result.review().getPlaceId()).isEqualTo("place-1");
    assertThat(result.review().getWeather()).isEqualTo("SUNNY");
    assertThat(result.photos()).isEmpty();
    verify(reviewEmbeddingService).generateAndSave(saved);
  }

  @Test
  void 코스_방문지_연결해서_리뷰_생성된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    ReviewCreateRequest request =
        request("place-1", petId, (short) 5, "코스 최고!", "SUNNY", coursePlaceId);
    Review saved = new Review("place-1", userId, petId, (short) 5, "코스 최고!", "SUNNY");
    saved.setCoursePlaceId(coursePlaceId);
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    ReviewDetail result = reviewService.createReview(userId, request);

    assertThat(result.review().getCoursePlaceId()).isEqualTo(coursePlaceId);
  }

  @Test
  void 사진과_함께_리뷰_생성시_순서대로_저장되고_응답에_실린다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID photo1 = UUID.randomUUID();
    UUID photo2 = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest(
            "place-1", petId, (short) 5, "사진 많음", "SUNNY", null, List.of(photo1, photo2));
    Review saved = new Review("place-1", userId, petId, (short) 5, "사진 많음", "SUNNY");
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(photoService.getOwnedPhoto(any(), any()))
        .thenReturn(new Photo(userId, null, "review/u/a.jpg", null));
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    Photo p1 = new Photo(userId, null, "review/u/1.jpg", LocalDate.of(2026, 7, 1));
    Photo p2 = new Photo(userId, null, "review/u/2.jpg", null);
    when(reviewPhotoRepository.findByReviewIdInOrderByPhotoOrderAsc(any()))
        .thenReturn(
            List.of(
                new ReviewPhoto(saved.getId(), p1.getId(), (short) 0),
                new ReviewPhoto(saved.getId(), p2.getId(), (short) 1)));
    when(photoRepository.findAllById(any())).thenReturn(List.of(p1, p2));
    when(photoService.issueDownloadUrl(any())).thenReturn(URI.create("https://bucket/x").toURL());

    ReviewDetail result = reviewService.createReview(userId, request);

    verify(reviewPhotoRepository, times(2)).save(any(ReviewPhoto.class));
    assertThat(result.photos()).hasSize(2);
    assertThat(result.photos().get(0).photoId()).isEqualTo(p1.getId());
  }

  @Test
  void 타인_사진_첨부시_예외발생하고_리뷰는_저장되지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID foreignPhoto = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest(
            "place-1", petId, (short) 4, "좋음", "SUNNY", null, List.of(foreignPhoto));
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(photoService.getOwnedPhoto(userId, foreignPhoto)).thenThrow(new PhotoNotFoundException());

    assertThatThrownBy(() -> reviewService.createReview(userId, request))
        .isInstanceOf(PhotoNotFoundException.class);

    verify(reviewRepository, never()).save(any());
    verify(reviewPhotoRepository, never()).save(any());
  }

  @Test
  void 날씨없이_리뷰_생성시_정상_저장된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    ReviewCreateRequest request = request("place-2", petId, (short) 3, "그냥 그랬어요", null, null);
    Review saved = new Review("place-2", userId, petId, (short) 3, "그냥 그랬어요", null);
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    ReviewDetail result = reviewService.createReview(userId, request);

    assertThat(result.review().getWeather()).isNull();
    verify(reviewEmbeddingService).generateAndSave(saved);
  }

  @Test
  void 타인_반려동물로_리뷰_생성시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID otherPetId = UUID.randomUUID();
    ReviewCreateRequest request = request("place-1", otherPetId, (short) 4, "좋았어요", null, null);
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

    List<ReviewDetail> result = reviewService.listPlaceReviews(placeId);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).review().getContents()).isEqualTo("리뷰1");
    assertThat(result.get(0).photos()).isEmpty();
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

  @Test
  void getMyAlbum은_내_리뷰_사진을_최신리뷰_사진순으로_펼쳐서_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();
    Review r1 = new Review("place-A", userId, UUID.randomUUID(), (short) 5, "리뷰1", "SUNNY");
    Photo p1 = new Photo(userId, null, "review/u/1.jpg", java.time.LocalDate.of(2026, 7, 1));
    Photo p2 = new Photo(userId, null, "review/u/2.jpg", null);
    when(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(r1));
    when(reviewPhotoRepository.findByReviewIdInOrderByPhotoOrderAsc(any()))
        .thenReturn(
            List.of(
                new ReviewPhoto(r1.getId(), p1.getId(), (short) 0),
                new ReviewPhoto(r1.getId(), p2.getId(), (short) 1)));
    when(photoRepository.findAllById(any())).thenReturn(List.of(p1, p2));
    when(photoService.issueDownloadUrl(any())).thenReturn(URI.create("https://bucket/x").toURL());

    List<AlbumItem> album = reviewService.getMyAlbum(userId);

    assertThat(album).hasSize(2);
    assertThat(album.get(0).photoId()).isEqualTo(p1.getId());
    assertThat(album.get(0).reviewId()).isEqualTo(r1.getId());
    assertThat(album.get(0).placeId()).isEqualTo("place-A");
  }

  @Test
  void getMyAlbum은_리뷰가_없으면_빈_리스트를_반환한다() {
    UUID userId = UUID.randomUUID();
    when(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

    assertThat(reviewService.getMyAlbum(userId)).isEmpty();
  }
}
