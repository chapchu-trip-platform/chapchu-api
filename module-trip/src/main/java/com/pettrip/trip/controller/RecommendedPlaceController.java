package com.pettrip.trip.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.trip.service.CourseService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommended-places")
public class RecommendedPlaceController {

  private final CourseService courseService;

  public RecommendedPlaceController(CourseService courseService) {
    this.courseService = courseService;
  }

  @PostMapping
  public List<RecommendedPlaceResponse> recommendPlaces(
      @CurrentUserId UUID userId, @Valid @RequestBody RecommendedPlaceRequest request) {
    return courseService
        .recommendPlaces(
            userId,
            request.petId(),
            request.lat(),
            request.lng(),
            resolveRadius(request.radiusMeters()),
            request.temperature(),
            request.humidity(),
            request.weatherStatus())
        .stream()
        .map(RecommendedPlaceResponse::from)
        .toList();
  }

  private int resolveRadius(int radiusMeters) {
    if (radiusMeters > 0) {
      return radiusMeters;
    }
    return 5000;
  }
}
