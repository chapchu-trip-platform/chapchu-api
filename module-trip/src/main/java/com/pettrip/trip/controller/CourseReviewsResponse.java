package com.pettrip.trip.controller;

import com.pettrip.trip.service.CourseService.CourseReviewInStop;
import com.pettrip.trip.service.CourseService.CourseReviewPhoto;
import com.pettrip.trip.service.CourseService.CourseReviewStop;
import com.pettrip.trip.service.CourseService.CourseReviewsDetail;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 코스 단위 리뷰·사진 묶음. 스탑별로 코스 주인이 쓴 리뷰(+사진)를 붙여 준다(없으면 review=null). */
public record CourseReviewsResponse(UUID courseId, List<StopItem> stops) {

  public record StopItem(
      UUID coursePlaceId,
      String externalPlaceId,
      String placeName,
      short visitOrder,
      ReviewItem review) {}

  public record ReviewItem(
      UUID reviewId,
      Short rating,
      String contents,
      String weather,
      LocalDateTime createdAt,
      List<PhotoItem> photos) {}

  public record PhotoItem(UUID photoId, String downloadUrl, LocalDate takenAt) {}

  public static CourseReviewsResponse from(CourseReviewsDetail detail) {
    return new CourseReviewsResponse(
        detail.courseId(), detail.stops().stream().map(CourseReviewsResponse::toStop).toList());
  }

  private static StopItem toStop(CourseReviewStop s) {
    return new StopItem(
        s.coursePlaceId(),
        s.externalPlaceId(),
        s.placeName(),
        s.visitOrder(),
        toReview(s.review()));
  }

  private static ReviewItem toReview(CourseReviewInStop r) {
    if (r == null) {
      return null;
    }
    return new ReviewItem(
        r.reviewId(),
        r.rating(),
        r.contents(),
        r.weather(),
        r.createdAt(),
        r.photos().stream().map(CourseReviewsResponse::toPhoto).toList());
  }

  private static PhotoItem toPhoto(CourseReviewPhoto p) {
    return new PhotoItem(p.photoId(), p.downloadUrl(), p.takenAt());
  }
}
