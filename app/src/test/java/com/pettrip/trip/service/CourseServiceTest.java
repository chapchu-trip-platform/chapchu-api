package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.pettrip.place.model.Place;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  @Mock private PlaceService placeService;
  @Mock private PlaceRepository placeRepository;
  @Mock private TravelCourseRepository travelCourseRepository;
  @Mock private CoursePlaceRepository coursePlaceRepository;
  @Mock private RouteOptimizationService routeOptimizationService;
  @Mock private PlaceRagService placeRagService;

  @InjectMocks private CourseService courseService;

  private Place samplePlace(String id, String name) {
    return new Place(
        id,
        null,
        name,
        null,
        "서울시",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        null,
        null,
        null);
  }

  private TravelCourse sampleCourse(UUID userId) {
    StartCourse start = new StartCourse("강남구", LocalDateTime.now());
    return new TravelCourse(userId, start, LocalDate.now());
  }

  private CoursePlace sampleCoursePlace(TravelCourse course, boolean finalPlace) {
    return new CoursePlace(course, "place-1", (short) 1, finalPlace);
  }

  @Test
  void 주변_장소로_코스를_생성한다() {
    List<Place> places = List.of(samplePlace("p1", "장소A"), samplePlace("p2", "장소B"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(placeRagService.rankByReviewSimilarity(any())).thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any())).thenReturn(List.of("p1", "p2"));
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(coursePlaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TravelCourse result =
        courseService.createCourse(
            UUID.randomUUID(),
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            5000,
            LocalDate.now(),
            "강남구");

    assertThat(result).isNotNull();
    assertThat(result.getTravelDate()).isEqualTo(LocalDate.now());
  }

  @Test
  void 마지막_장소에_finalPlace_true가_설정된다() {
    List<Place> places =
        List.of(samplePlace("p1", "A"), samplePlace("p2", "B"), samplePlace("p3", "C"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(placeRagService.rankByReviewSimilarity(any())).thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any())).thenReturn(List.of("p1", "p2", "p3"));
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var saved = new ArrayList<CoursePlace>();
    when(coursePlaceRepository.save(any(CoursePlace.class)))
        .thenAnswer(
            inv -> {
              saved.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    courseService.createCourse(
        UUID.randomUUID(),
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        5000,
        LocalDate.now(),
        "강남구");

    assertThat(saved).hasSize(3);
    assertThat(saved.get(0).isFinalPlace()).isFalse();
    assertThat(saved.get(2).isFinalPlace()).isTrue();
  }

  @Test
  void 주변_장소_없을때_코스생성_예외발생한다() {
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any())).thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any())).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                courseService.createCourse(
                    UUID.randomUUID(),
                    new BigDecimal("0"),
                    new BigDecimal("0"),
                    5000,
                    LocalDate.now(),
                    "외딴곳"))
        .isInstanceOf(NoPlacesFoundException.class);
  }

  @Test
  void 존재하지_않는_코스_조회시_예외가_발생한다() {
    UUID courseId = UUID.randomUUID();
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.getCourse(UUID.randomUUID(), courseId))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void 타인_코스_조회시_예외발생한다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(ownerId);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.getCourse(otherId, courseId))
        .isInstanceOf(CourseNotOwnerException.class);
  }

  @Test
  void 내_코스_목록이_반환된다() {
    UUID userId = UUID.randomUUID();
    List<TravelCourse> courses = List.of(sampleCourse(userId), sampleCourse(userId));
    when(travelCourseRepository.findByUserIdWithPlaces(userId)).thenReturn(courses);

    List<TravelCourse> result = courseService.listMyCourses(userId);

    assertThat(result).hasSize(2);
  }

  @Test
  void 방문_체크인시_isVisited와_visitedAt이_설정된다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, false);
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));
    when(placeRepository.findById("place-1")).thenReturn(Optional.of(samplePlace("place-1", "장소")));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    assertThat(coursePlace.isVisited()).isTrue();
    assertThat(coursePlace.getVisitedAt()).isNotNull();
  }

  @Test
  void 마지막_장소_체크인시_코스완료처리된다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, true);
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));
    when(placeRepository.findById("place-1")).thenReturn(Optional.of(samplePlace("place-1", "장소")));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    assertThat(course.isCompleted()).isTrue();
  }

  @Test
  void 타인_코스_장소_체크인시_예외발생한다() {
    UUID otherId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, otherId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.visitPlace(otherId, coursePlaceId, 37.5, 127.0))
        .isInstanceOf(CourseNotOwnerException.class);
  }

  @Test
  void 이미_체크인된_장소_재체크인시_멱등처리된다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, false);
    coursePlace.markVisited();
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    assertThat(coursePlace.isVisited()).isTrue();
    assertThat(course.isCompleted()).isFalse();
  }

  @Test
  void 장소에서_500m_초과시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, false);
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));
    when(placeRepository.findById("place-1")).thenReturn(Optional.of(samplePlace("place-1", "장소")));

    assertThatThrownBy(() -> courseService.visitPlace(userId, coursePlaceId, 0.0, 0.0))
        .isInstanceOf(TooFarFromPlaceException.class);
  }
}
