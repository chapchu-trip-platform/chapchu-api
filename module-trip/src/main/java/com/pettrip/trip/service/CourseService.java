package com.pettrip.trip.service;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.StartCourse;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CoursePlaceRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

  private final PlaceService placeService;
  private final PlaceRepository placeRepository;
  private final TravelCourseRepository travelCourseRepository;
  private final CoursePlaceRepository coursePlaceRepository;

  public CourseService(
      PlaceService placeService,
      PlaceRepository placeRepository,
      TravelCourseRepository travelCourseRepository,
      CoursePlaceRepository coursePlaceRepository) {
    this.placeService = placeService;
    this.placeRepository = placeRepository;
    this.travelCourseRepository = travelCourseRepository;
    this.coursePlaceRepository = coursePlaceRepository;
  }

  @Transactional
  public TravelCourse createCourse(
      UUID userId,
      BigDecimal lat,
      BigDecimal lng,
      int radiusMeters,
      LocalDate travelDate,
      String startLocation) {
    List<Place> places = placeService.searchNearby(lat, lng, radiusMeters);

    StartCourse startCourse = new StartCourse(startLocation, LocalDateTime.now());
    TravelCourse course = new TravelCourse(userId, startCourse, travelDate);
    travelCourseRepository.save(course);

    for (int i = 0; i < places.size(); i++) {
      boolean isLast = (i == places.size() - 1);
      coursePlaceRepository.save(
          new CoursePlace(course, places.get(i).getExternalPlaceId(), (short) (i + 1), isLast));
    }

    return course;
  }

  @Transactional(readOnly = true)
  public TravelCourseDetail getCourse(UUID courseId) {
    TravelCourse course =
        travelCourseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);

    List<CoursePlace> coursePlaces =
        coursePlaceRepository.findByCourseIdOrderByVisitOrderAsc(courseId);

    List<String> placeIds = coursePlaces.stream().map(CoursePlace::getExternalPlaceId).toList();

    Map<String, Place> placeMap =
        placeRepository.findAllById(placeIds).stream()
            .collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity()));

    return new TravelCourseDetail(course, coursePlaces, placeMap);
  }

  public record TravelCourseDetail(
      TravelCourse course, List<CoursePlace> coursePlaces, Map<String, Place> placeMap) {}
}
