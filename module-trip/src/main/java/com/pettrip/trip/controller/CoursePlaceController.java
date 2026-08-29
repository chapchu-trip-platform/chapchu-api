package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.CourseService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/course-places")
public class CoursePlaceController {

  private final CourseService courseService;

  public CoursePlaceController(CourseService courseService) {
    this.courseService = courseService;
  }

  @PatchMapping("/{coursePlaceId}/visit")
  @ResponseStatus(HttpStatus.OK)
  public void visitPlace(@CurrentUserId UUID userId, @PathVariable UUID coursePlaceId) {
    courseService.visitPlace(userId, coursePlaceId);
  }
}
