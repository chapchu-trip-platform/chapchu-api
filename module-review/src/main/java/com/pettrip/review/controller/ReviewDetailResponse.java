package com.pettrip.review.controller;

import com.pettrip.review.model.Review;
import com.pettrip.review.service.ReviewFullDetail;
import com.pettrip.review.service.ReviewPhotoView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 리뷰 단건 상세 응답. 목록용 {@link ReviewResponse}와 달리 UUID 대신 이름을 채워 준다.
 *
 * <p>author·pet·place·course는 각각 없을 수 있어 null을 그대로 내려보낸다. 기본 이미지·빈 값 표시는 화면 몫이다.
 */
public record ReviewDetailResponse(
    UUID id,
    Short rating,
    String contents,
    String weather,
    int recommendationCount,
    LocalDateTime createdAt,
    UUID coursePlaceId,
    Author author,
    Pet pet,
    Place place,
    Course course,
    List<ReviewPhotoView> photos) {

  public record Author(UUID userId, String nickname, PhotoRef profilePhoto) {}

  public record Pet(UUID petId, String petName, String breedName, PhotoRef profilePhoto) {}

  public record Place(String externalPlaceId, String placeName, String address) {}

  public record Course(UUID courseId, LocalDate travelDate) {}

  public record PhotoRef(UUID photoId, String downloadUrl) {}

  public static ReviewDetailResponse of(ReviewFullDetail detail) {
    Review review = detail.review();
    return new ReviewDetailResponse(
        review.getId(),
        review.getRating(),
        review.getContents(),
        review.getWeather(),
        review.getRecommendationCount(),
        review.getCreatedAt(),
        review.getCoursePlaceId(),
        toAuthor(detail.author()),
        toPet(detail.pet()),
        toPlace(detail.place()),
        toCourse(detail.course()),
        detail.photos());
  }

  private static Author toAuthor(ReviewFullDetail.Author author) {
    if (author == null) {
      return null;
    }
    return new Author(author.userId(), author.nickname(), toPhotoRef(author.profilePhoto()));
  }

  private static Pet toPet(ReviewFullDetail.PetInfo pet) {
    if (pet == null) {
      return null;
    }
    return new Pet(pet.petId(), pet.petName(), pet.breedName(), toPhotoRef(pet.profilePhoto()));
  }

  private static Place toPlace(ReviewFullDetail.PlaceInfo place) {
    if (place == null) {
      return null;
    }
    return new Place(place.externalPlaceId(), place.placeName(), place.address());
  }

  private static Course toCourse(ReviewFullDetail.CourseInfo course) {
    if (course == null) {
      return null;
    }
    return new Course(course.courseId(), course.travelDate());
  }

  private static PhotoRef toPhotoRef(ReviewFullDetail.PhotoRef ref) {
    if (ref == null) {
      return null;
    }
    return new PhotoRef(ref.photoId(), ref.downloadUrl());
  }
}
