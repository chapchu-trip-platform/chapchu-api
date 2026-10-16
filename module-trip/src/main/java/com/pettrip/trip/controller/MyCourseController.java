package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.CourseService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/courses")
public class MyCourseController {

  private final CourseService courseService;

  public MyCourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping
  public List<MyCourseResponse> listMyCourses(@CurrentUserId UUID userId) {
    return courseService.listMyCourses(userId).stream().map(MyCourseResponse::from).toList();
  }
}
