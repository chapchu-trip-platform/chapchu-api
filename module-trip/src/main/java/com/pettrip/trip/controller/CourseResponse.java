package com.pettrip.trip.controller;

import com.pettrip.place.model.Place;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.service.CourseService.TravelCourseDetail;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CourseResponse(
    UUID courseId, LocalDate travelDate, String startLocation, List<CoursePlaceItem> places) {

  public record CoursePlaceItem(
      String externalPlaceId, String placeName, short visitOrder, boolean finalPlace) {}

  public static CourseResponse from(TravelCourseDetail detail) {
    TravelCourse course = detail.course();
    List<CoursePlaceItem> items =
        detail.coursePlaces().stream()
            .map(cp -> toItem(cp, detail.placeMap().get(cp.getExternalPlaceId())))
            .toList();
    return new CourseResponse(
        course.getId(),
        course.getTravelDate(),
        course.getStartCourse().getStartCourseLocation(),
        items);
  }

  private static CoursePlaceItem toItem(CoursePlace cp, Place place) {
    String name = place != null ? place.getPlaceName() : cp.getExternalPlaceId();
    return new CoursePlaceItem(
        cp.getExternalPlaceId(), name, cp.getVisitOrder(), cp.isFinalPlace());
  }
}
