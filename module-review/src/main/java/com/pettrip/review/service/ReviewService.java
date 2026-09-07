package com.pettrip.review.service;

import com.pettrip.pet.repository.PetRepository;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.repository.PhotoRepository;
import com.pettrip.photo.service.PhotoService;
import com.pettrip.review.controller.ReviewCreateRequest;
import com.pettrip.review.model.Review;
import com.pettrip.review.model.ReviewPhoto;
import com.pettrip.review.model.ReviewRecommendation;
import com.pettrip.review.repository.ReviewPhotoRepository;
import com.pettrip.review.repository.ReviewRecommendationRepository;
import com.pettrip.review.repository.ReviewRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
  private final ReviewPhotoRepository reviewPhotoRepository;
  private final PhotoRepository photoRepository;
  private final PhotoService photoService;

  public ReviewService(
      ReviewRepository reviewRepository,
      ReviewRecommendationRepository reviewRecommendationRepository,
      ReviewEmbeddingService reviewEmbeddingService,
      PetRepository petRepository,
      ReviewPhotoRepository reviewPhotoRepository,
      PhotoRepository photoRepository,
      PhotoService photoService) {
    this.reviewRepository = reviewRepository;
    this.reviewRecommendationRepository = reviewRecommendationRepository;
    this.reviewEmbeddingService = reviewEmbeddingService;
    this.petRepository = petRepository;
    this.reviewPhotoRepository = reviewPhotoRepository;
    this.photoRepository = photoRepository;
    this.photoService = photoService;
  }

  public ReviewDetail createReview(UUID userId, ReviewCreateRequest request) {
    if (!petRepository.existsByIdAndUserId(request.petId(), userId)) {
      throw new PetNotOwnedException();
    }
    List<UUID> photoIds = request.photoIds() == null ? List.of() : request.photoIds();
    for (UUID photoId : photoIds) {
      photoService.getOwnedPhoto(userId, photoId);
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
    for (int i = 0; i < photoIds.size(); i++) {
      reviewPhotoRepository.save(new ReviewPhoto(saved.getId(), photoIds.get(i), (short) i));
    }
    log.info(
        "[리뷰] 작성 완료 — reviewId={}, placeId={}, petId={}, rating={}, 사진 {}장",
        saved.getId(),
        saved.getPlaceId(),
        saved.getPetId(),
        saved.getRating(),
        photoIds.size());
    reviewEmbeddingService.generateAndSave(saved);
    return assemble(List.of(saved)).get(0);
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

  public List<ReviewDetail> listMyReviews(UUID userId) {
    return assemble(reviewRepository.findByUserIdOrderByCreatedAtDesc(userId));
  }

  /** 내 앨범: 내가 쓴 리뷰들의 사진을 최신 리뷰 → 사진 순서로 펼쳐서 반환. */
  public List<AlbumItem> getMyAlbum(UUID userId) {
    List<Review> reviews = reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
    if (reviews.isEmpty()) {
      return List.of();
    }
    List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
    List<ReviewPhoto> links = reviewPhotoRepository.findByReviewIdInOrderByPhotoOrderAsc(reviewIds);
    Map<UUID, List<ReviewPhoto>> linksByReview =
        links.stream()
            .collect(
                Collectors.groupingBy(
                    ReviewPhoto::getReviewId, LinkedHashMap::new, Collectors.toList()));
    List<UUID> photoIds = links.stream().map(ReviewPhoto::getPhotoId).distinct().toList();
    Map<UUID, Photo> photoMap =
        photoRepository.findAllById(photoIds).stream()
            .collect(Collectors.toMap(Photo::getId, Function.identity()));

    List<AlbumItem> items = new ArrayList<>();
    for (Review review : reviews) {
      for (ReviewPhoto link : linksByReview.getOrDefault(review.getId(), List.of())) {
        Photo photo = photoMap.get(link.getPhotoId());
        if (photo == null) {
          continue;
        }
        items.add(
            new AlbumItem(
                photo.getId(),
                photoService.issueDownloadUrl(photo.getPhotoUrl()).toString(),
                photo.getTakenAt(),
                review.getId(),
                review.getPlaceId()));
      }
    }
    return items;
  }

  public List<ReviewDetail> listPlaceReviews(String placeId) {
    return assemble(reviewRepository.findByPlaceIdOrderByCreatedAtDesc(placeId));
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

  private List<ReviewDetail> assemble(List<Review> reviews) {
    if (reviews.isEmpty()) {
      return List.of();
    }
    List<UUID> reviewIds = reviews.stream().map(Review::getId).toList();
    List<ReviewPhoto> links = reviewPhotoRepository.findByReviewIdInOrderByPhotoOrderAsc(reviewIds);
    List<UUID> photoIds = links.stream().map(ReviewPhoto::getPhotoId).distinct().toList();
    Map<UUID, Photo> photoMap =
        photoRepository.findAllById(photoIds).stream()
            .collect(Collectors.toMap(Photo::getId, Function.identity()));

    Map<UUID, List<ReviewPhotoView>> byReview = new LinkedHashMap<>();
    for (ReviewPhoto link : links) {
      Photo photo = photoMap.get(link.getPhotoId());
      if (photo == null) {
        continue;
      }
      ReviewPhotoView view =
          new ReviewPhotoView(
              photo.getId(),
              photoService.issueDownloadUrl(photo.getPhotoUrl()).toString(),
              photo.getTakenAt());
      byReview.computeIfAbsent(link.getReviewId(), key -> new ArrayList<>()).add(view);
    }

    return reviews.stream()
        .map(review -> new ReviewDetail(review, byReview.getOrDefault(review.getId(), List.of())))
        .toList();
  }
}
