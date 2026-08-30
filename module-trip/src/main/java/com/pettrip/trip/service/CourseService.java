package com.pettrip.trip.service;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.recommendation.service.PlaceInfo;
import com.pettrip.recommendation.service.PlaceRagService;
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
import java.util.Objects;
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
  private final PlaceRagService placeRagService;
  private final TravelCourseRepository travelCourseRepository;
  private final CoursePlaceRepository coursePlaceRepository;

  public CourseService(
      PlaceService placeService,
      PlaceRepository placeRepository,
      RouteOptimizationService routeOptimizationService,
      PlaceRagService placeRagService,
      TravelCourseRepository travelCourseRepository,
      CoursePlaceRepository coursePlaceRepository) {
    this.placeService = placeService;
    this.placeRepository = placeRepository;
    this.routeOptimizationService = routeOptimizationService;
    this.placeRagService = placeRagService;
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

    Map<String, Place> placeMap =
        places.stream().collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity()));

    List<String> rawIds = places.stream().map(Place::getExternalPlaceId).toList();
    List<String> ragRankedIds = placeRagService.rankByReviewSimilarity(rawIds);

    List<PlaceInfo> placeInfos =
        ragRankedIds.stream()
            .map(placeMap::get)
            .filter(Objects::nonNull)
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
  public void visitPlace(UUID userId, UUID coursePlaceId, double lat, double lng) {
    CoursePlace coursePlace =
        coursePlaceRepository
            .findByIdAndCourseUserId(coursePlaceId, userId)
            .orElseThrow(CourseNotOwnerException::new);
    if (coursePlace.isVisited()) {
      return;
    }
    Place place =
        placeRepository
            .findById(coursePlace.getExternalPlaceId())
            .orElseThrow(CourseNotOwnerException::new);
    double distanceM =
        haversineMeters(
            lat, lng, place.getLatitude().doubleValue(), place.getLongitude().doubleValue());
    if (distanceM > 500) {
      throw new TooFarFromPlaceException();
    }
    coursePlace.markVisited();
    if (coursePlace.isFinalPlace()) {
      coursePlace.getCourse().complete();
    }
  }

  private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
    double r = 6_371_000;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  public record TravelCourseDetail(
      TravelCourse course, List<CoursePlace> coursePlaces, Map<String, Place> placeMap) {}
}
