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
    String name = place != null ? place.getPlaceName() : cp.getExternalPlaceId();
    String imageUrl = place != null ? place.getPlaceImageUrl() : null;
    BigDecimal lat = place != null ? place.getLatitude() : null;
    BigDecimal lng = place != null ? place.getLongitude() : null;
    PetPolicySummary petPolicy = policy != null ? toPolicySummary(policy) : null;
    return new CoursePlaceItem(
        cp.getId(),
        cp.getExternalPlaceId(),
        name,
        imageUrl,
        lat,
        lng,
        cp.getVisitOrder(),
        cp.isFinalPlace(),
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
