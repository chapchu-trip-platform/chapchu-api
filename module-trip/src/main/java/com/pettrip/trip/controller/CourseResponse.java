package com.pettrip.trip.controller;

import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.service.CourseService.TravelCourseDetail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
    UUID courseId,
    LocalDate travelDate,
    String startLocation,
    String endLocation,
    List<CoursePlaceItem> places) {

  public record CoursePlaceItem(
      UUID coursePlaceId,
      String externalPlaceId,
      String placeName,
      String placeImageUrl,
      BigDecimal latitude,
      BigDecimal longitude,
      short visitOrder,
      boolean finalPlace,
      String reason,
      Boolean petAllowed,
      PetPolicySummary petPolicy) {}

  public record PetPolicySummary(
      String allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      String indoorOutdoorType,
      String placeCaution) {}

  public static CourseResponse from(TravelCourseDetail detail) {
    TravelCourse course = detail.course();
    List<CoursePlaceItem> items =
        detail.coursePlaces().stream()
            .map(
                cp ->
                    toItem(
                        cp,
                        detail.placeMap().get(cp.getExternalPlaceId()),
                        detail.policyMap().get(cp.getExternalPlaceId())))
            .toList();
    return new CourseResponse(
        course.getId(),
        course.getTravelDate(),
        course.getStartLocation(),
        course.getEndLocation(),
        items);
  }

  private static CoursePlaceItem toItem(CoursePlace cp, Place place, PlacePetPolicy policy) {
    // 기본값은 도착지 스탑의 비정규화 필드(place가 없을 때). 중간 스탑은 place 값으로 덮어쓴다.
    String name = cp.getPlaceName();
    String imageUrl = null;
    BigDecimal lat = cp.getLatitude();
    BigDecimal lng = cp.getLongitude();
    if (place != null) {
      name = place.getPlaceName();
      imageUrl = place.getPlaceImageUrl();
      lat = place.getLatitude();
      lng = place.getLongitude();
    }
    PetPolicySummary petPolicy = null;
    if (policy != null) {
      petPolicy = toPolicySummary(policy);
    }
    return new CoursePlaceItem(
        cp.getId(),
        cp.getExternalPlaceId(),
        name,
        imageUrl,
        lat,
        lng,
        cp.getVisitOrder(),
        cp.isFinalPlace(),
        cp.getReason(),
        cp.getPetAllowed(),
        petPolicy);
  }

  private static PetPolicySummary toPolicySummary(PlacePetPolicy p) {
    return new PetPolicySummary(
        p.getAllowedPetSize() != null ? p.getAllowedPetSize().name() : null,
        p.getLeashRequired(),
        p.getCarrierRequired(),
        p.getIndoorOutdoorType() != null ? p.getIndoorOutdoorType().name() : null,
        p.getPlaceCaution());
  }
}
