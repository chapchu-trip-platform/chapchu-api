package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.CourseService;
import com.pettrip.trip.service.CourseService.TravelCourseDetail;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CourseResponse createCourse(
      @CurrentUserId UUID userId, @Valid @RequestBody CreateCourseRequest request) {
    var course =
        courseService.createCourse(
            userId,
            request.lat(),
            request.lng(),
            request.radiusMeters() > 0 ? request.radiusMeters() : 5000,
            request.travelDate(),
            request.startLocation());
    TravelCourseDetail detail = courseService.getCourse(userId, course.getId());
    return CourseResponse.from(detail);
  }

  @GetMapping("/{courseId}")
  public CourseResponse getCourse(@CurrentUserId UUID userId, @PathVariable UUID courseId) {
    return CourseResponse.from(courseService.getCourse(userId, courseId));
  }
}
