package com.pettrip.trip.controller;

import com.pettrip.trip.model.TravelCourse;
import java.time.LocalDate;
import java.util.UUID;

public record MyCourseResponse(
    UUID courseId,
    LocalDate travelDate,
    String startLocation,
    boolean isCompleted,
    int placeCount) {

  public static MyCourseResponse from(TravelCourse course) {
    return new MyCourseResponse(
        course.getId(),
        course.getTravelDate(),
        course.getStartLocation(),
        course.isCompleted(),
        course.getCoursePlaces().size());
  }
}
