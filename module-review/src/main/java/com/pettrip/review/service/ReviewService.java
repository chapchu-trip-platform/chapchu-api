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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public ReviewService(
      ReviewRepository reviewRepository,
      ReviewRecommendationRepository reviewRecommendationRepository,
      ReviewEmbeddingService reviewEmbeddingService,
      PetRepository petRepository,
      ReviewPhotoRepository reviewPhotoRepository,
      PhotoRepository photoRepository,
      PhotoService photoService,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.reviewRepository = reviewRepository;
    this.reviewRecommendationRepository = reviewRecommendationRepository;
    this.reviewEmbeddingService = reviewEmbeddingService;
    this.petRepository = petRepository;
    this.reviewPhotoRepository = reviewPhotoRepository;
    this.photoRepository = photoRepository;
    this.photoService = photoService;
    this.jdbcTemplate = jdbcTemplate;
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

  /**
   * 리뷰 1건에 붙는 주변 정보. users·pets·places·travel_courses는 module-review 소관이 아니라 JdbcTemplate로 직접 읽는다
   * (COMPLETE_IF_ALL_REVIEWED_SQL과 같은 방식 — 모듈 순환을 피한다).
   *
   * <p>전부 LEFT JOIN이다. 작성자가 탈퇴했거나(user_id NULL) places에 없는 장소여도 리뷰 본문은 내려가야 한다.
   */
  private static final String REVIEW_ENRICH_SQL =
      """
      SELECT u.user_id, u.nickname,
             u.profile_photo_id AS user_photo_id, upp.photo_url AS user_photo_key,
             p.pet_id, p.pet_name, b.breed_name,
             p.profile_photo_id AS pet_photo_id, ppp.photo_url AS pet_photo_key,
             pl.place_name, pl.address,
             cp.course_id, tc.travel_date
      FROM reviews r
      LEFT JOIN users u ON u.user_id = r.user_id
      LEFT JOIN photos upp ON upp.photo_id = u.profile_photo_id
      LEFT JOIN pets p ON p.pet_id = r.pet_id
      LEFT JOIN breeds b ON b.breed_id = p.breed_id
      LEFT JOIN photos ppp ON ppp.photo_id = p.profile_photo_id
      LEFT JOIN places pl ON pl.external_place_id = r.place_id
      LEFT JOIN course_places cp ON cp.course_place_id = r.course_place_id
      LEFT JOIN travel_courses tc ON tc.course_id = cp.course_id
      WHERE r.review_id = :reviewId
      """;

  private static final RowMapper<ReviewEnrichRow> ENRICH_ROW_MAPPER =
      (rs, n) ->
          new ReviewEnrichRow(
              rs.getObject("user_id", UUID.class),
              rs.getString("nickname"),
              rs.getObject("user_photo_id", UUID.class),
              rs.getString("user_photo_key"),
              rs.getObject("pet_id", UUID.class),
              rs.getString("pet_name"),
              rs.getString("breed_name"),
              rs.getObject("pet_photo_id", UUID.class),
              rs.getString("pet_photo_key"),
              rs.getString("place_name"),
              rs.getString("address"),
              rs.getObject("course_id", UUID.class),
              rs.getObject("travel_date", LocalDate.class));

  /** 리뷰 1건에 붙는 주변 정보 한 줄. LEFT JOIN이라 어느 칸이든 null일 수 있다. */
  record ReviewEnrichRow(
      UUID userId,
      String nickname,
      UUID userPhotoId,
      String userPhotoKey,
      UUID petId,
      String petName,
      String breedName,
      UUID petPhotoId,
      String petPhotoKey,
      String placeName,
      String address,
      UUID courseId,
      LocalDate travelDate) {}

  /** 리뷰 단건 상세. 작성자·반려동물·장소 이름까지 채운다(SNS 카드·게시판 상세용). */
  @Transactional(readOnly = true)
  public ReviewFullDetail getReview(UUID reviewId) {
    Review review = findReview(reviewId);
    List<ReviewPhotoView> photos = assemble(List.of(review)).get(0).photos();
    List<ReviewEnrichRow> rows =
        jdbcTemplate.query(
            REVIEW_ENRICH_SQL,
            new MapSqlParameterSource().addValue("reviewId", reviewId),
            ENRICH_ROW_MAPPER);
    if (rows.isEmpty()) {
      return new ReviewFullDetail(review, null, null, place(null, review), null, photos);
    }
    ReviewEnrichRow row = rows.get(0);
    return new ReviewFullDetail(
        review, author(row), pet(row), place(row, review), course(row), photos);
  }

  private ReviewFullDetail.Author author(ReviewEnrichRow row) {
    if (row.userId() == null) {
      return null;
    }
    return new ReviewFullDetail.Author(
        row.userId(), row.nickname(), photoRef(row.userPhotoId(), row.userPhotoKey()));
  }

  private ReviewFullDetail.PetInfo pet(ReviewEnrichRow row) {
    if (row.petId() == null) {
      return null;
    }
    return new ReviewFullDetail.PetInfo(
        row.petId(), row.petName(), row.breedName(), photoRef(row.petPhotoId(), row.petPhotoKey()));
  }

  /** externalPlaceId는 리뷰가 들고 있어 항상 채운다. places에 없는 장소면 이름·주소만 null이다. */
  private ReviewFullDetail.PlaceInfo place(ReviewEnrichRow row, Review review) {
    if (row == null) {
      return new ReviewFullDetail.PlaceInfo(review.getPlaceId(), null, null);
    }
    return new ReviewFullDetail.PlaceInfo(review.getPlaceId(), row.placeName(), row.address());
  }

  private ReviewFullDetail.CourseInfo course(ReviewEnrichRow row) {
    if (row.courseId() == null) {
      return null;
    }
    return new ReviewFullDetail.CourseInfo(row.courseId(), row.travelDate());
  }

  private ReviewFullDetail.PhotoRef photoRef(UUID photoId, String photoKey) {
    if (photoId == null || photoKey == null) {
      return null;
    }
    return new ReviewFullDetail.PhotoRef(
        photoId, photoService.issueDownloadUrl(photoKey).toString());
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
