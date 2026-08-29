package com.pettrip.trip.service;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.recommendation.service.PlaceInfo;
import com.pettrip.recommendation.service.RouteOptimizationService;
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
  private final RouteOptimizationService routeOptimizationService;
  private final TravelCourseRepository travelCourseRepository;
  private final CoursePlaceRepository coursePlaceRepository;

  public CourseService(
      PlaceService placeService,
      PlaceRepository placeRepository,
      RouteOptimizationService routeOptimizationService,
      TravelCourseRepository travelCourseRepository,
      CoursePlaceRepository coursePlaceRepository) {
    this.placeService = placeService;
    this.placeRepository = placeRepository;
    this.routeOptimizationService = routeOptimizationService;
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

    List<PlaceInfo> placeInfos =
        places.stream()
            .map(
                p ->
                    new PlaceInfo(
                        p.getExternalPlaceId(),
                        p.getPlaceName(),
                        p.getAddress(),
                        p.getLatitude(),
                        p.getLongitude()))
            .toList();

    List<String> orderedIds = routeOptimizationService.optimizeOrder(placeInfos);
    if (orderedIds.isEmpty()) {
      throw new NoPlacesFoundException();
    }

    StartCourse startCourse = new StartCourse(startLocation, LocalDateTime.now());
    TravelCourse course = new TravelCourse(userId, startCourse, travelDate);
    travelCourseRepository.save(course);

    for (int i = 0; i < orderedIds.size(); i++) {
      boolean isLast = (i == orderedIds.size() - 1);
      String placeId = orderedIds.get(i);
      coursePlaceRepository.save(new CoursePlace(course, placeId, (short) (i + 1), isLast));
    }

    return course;
  }

  @Transactional(readOnly = true)
  public TravelCourseDetail getCourse(UUID userId, UUID courseId) {
    TravelCourse course =
        travelCourseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (!userId.equals(course.getUserId())) {
      throw new CourseNotOwnerException();
    }

    List<CoursePlace> coursePlaces =
        coursePlaceRepository.findByCourseIdOrderByVisitOrderAsc(courseId);

    List<String> placeIds = coursePlaces.stream().map(CoursePlace::getExternalPlaceId).toList();

    Map<String, Place> placeMap =
        placeRepository.findAllById(placeIds).stream()
            .collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity()));

    return new TravelCourseDetail(course, coursePlaces, placeMap);
  }

  @Transactional(readOnly = true)
  public List<TravelCourse> listMyCourses(UUID userId) {
    return travelCourseRepository.findByUserIdWithPlaces(userId);
  }

  @Transactional
  public void visitPlace(UUID userId, UUID coursePlaceId) {
    CoursePlace coursePlace =
        coursePlaceRepository
            .findByIdAndCourseUserId(coursePlaceId, userId)
            .orElseThrow(CourseNotOwnerException::new);
    if (coursePlace.isVisited()) {
      return;
    }
    coursePlace.markVisited();
    if (coursePlace.isFinalPlace()) {
      coursePlace.getCourse().complete();
    }
  }

  public record TravelCourseDetail(
      TravelCourse course, List<CoursePlace> coursePlaces, Map<String, Place> placeMap) {}
}
