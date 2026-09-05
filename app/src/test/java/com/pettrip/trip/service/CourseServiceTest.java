package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.pet.service.PetNotFoundException;
import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.recommendation.service.PlaceInfo;
import com.pettrip.recommendation.service.PlaceRagService;
import com.pettrip.recommendation.service.RouteOptimizationService;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CoursePlaceRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import com.pettrip.trip.service.CourseService.RecommendedPlaceResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseServiceTest {

  @Mock private PlaceService placeService;
  @Mock private PlaceRepository placeRepository;
  @Mock private PlacePetPolicyRepository petPolicyRepository;
  @Mock private PetRepository petRepository;
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

  private Place samplePlace(String id, String name, BigDecimal lat, BigDecimal lng) {
    return new Place(id, null, name, null, "서울시", lat, lng, null, null, null);
  }

  private Pet samplePet(UUID userId) {
    return new Pet(userId, null, "초코", PetSize.SMALL, 3);
  }

  private TravelCourse sampleCourse(UUID userId) {
    return new TravelCourse(
        userId,
        "강남구",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        "종로구",
        new BigDecimal("37.6"),
        new BigDecimal("126.9"),
        LocalDate.now());
  }

  private CoursePlace sampleCoursePlace(TravelCourse course, boolean finalPlace) {
    return new CoursePlace(course, "place-1", (short) 1, finalPlace);
  }

  private void mockPetAndPolicy(UUID userId, UUID petId) {
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(petRepository.findById(petId)).thenReturn(Optional.of(samplePet(userId)));
    when(petPolicyRepository.findAllById(any()))
        .thenAnswer(
            inv -> {
              Iterable<String> ids = inv.getArgument(0);
              List<PlacePetPolicy> policies = new ArrayList<>();
              for (String id : ids) {
                policies.add(
                    new PlacePetPolicy(
                        samplePlace(id, "장소"), AllowedPetSize.ALL, null, null, null, null, null));
              }
              return policies;
            });
  }

  @Test
  void 주변_장소를_추천한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);

    List<Place> places = List.of(samplePlace("p1", "장소A"), samplePlace("p2", "장소B"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any(), any(), any(), any(), any()))
        .thenReturn(List.of("p1", "p2"));

    List<RecommendedPlaceResult> result =
        courseService.recommendPlaces(
            userId, petId, new BigDecimal("37.5"), new BigDecimal("127.0"), 5000, null, null, null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).place().getExternalPlaceId()).isEqualTo("p1");
    assertThat(result.get(0).categoryLabel()).isNotNull();
  }

  @Test
  void 추천_장소_없을때_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);

    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any(), any(), any(), any(), any()))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                courseService.recommendPlaces(
                    userId,
                    petId,
                    new BigDecimal("0"),
                    new BigDecimal("0"),
                    5000,
                    null,
                    null,
                    null))
        .isInstanceOf(NoPlacesFoundException.class);
  }

  @Test
  void 타인의_petId로_장소추천시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                courseService.recommendPlaces(
                    userId,
                    petId,
                    new BigDecimal("37.5"),
                    new BigDecimal("127.0"),
                    5000,
                    null,
                    null,
                    null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void 중간좌표_검색으로_코스를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt()))
        .thenAnswer(
            inv -> {
              BigDecimal lat = inv.getArgument(0);
              BigDecimal lng = inv.getArgument(1);
              return List.of(samplePlace("p-" + lng.toPlainString(), "장소", lat, lng));
            });
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.selectAndOrder(
            any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              List<PlaceInfo> startGroup = inv.getArgument(0);
              List<List<PlaceInfo>> middleGroups = inv.getArgument(1);
              List<PlaceInfo> endGroup = inv.getArgument(2);
              List<String> ids = new ArrayList<>();
              if (!startGroup.isEmpty()) {
                ids.add(startGroup.get(0).id());
              }
              for (List<PlaceInfo> mg : middleGroups) {
                if (!mg.isEmpty()) {
                  ids.add(mg.get(0).id());
                }
              }
              if (!endGroup.isEmpty()) {
                ids.add(endGroup.get(0).id());
              }
              return ids;
            });
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(coursePlaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    TravelCourse result =
        courseService.createCourse(
            userId,
            petId,
            LocalDate.now(),
            "강남구",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "종로구",
            new BigDecimal("37.6"),
            new BigDecimal("126.9"),
            4,
            (short) 25,
            (short) 60,
            "맑음");

    assertThat(result).isNotNull();
    assertThat(result.getTravelDate()).isEqualTo(LocalDate.now());
    assertThat(result.getStartLocation()).isEqualTo("강남구");
    assertThat(result.getEndLocation()).isEqualTo("종로구");
  }

  @Test
  void 마지막_장소에_finalPlace_true가_설정된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt()))
        .thenAnswer(
            inv -> {
              BigDecimal lat = inv.getArgument(0);
              BigDecimal lng = inv.getArgument(1);
              return List.of(samplePlace("p-" + lng.toPlainString(), "장소", lat, lng));
            });
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.selectAndOrder(
            any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              List<PlaceInfo> startGroup = inv.getArgument(0);
              List<List<PlaceInfo>> middleGroups = inv.getArgument(1);
              List<PlaceInfo> endGroup = inv.getArgument(2);
              List<String> ids = new ArrayList<>();
              if (!startGroup.isEmpty()) {
                ids.add(startGroup.get(0).id());
              }
              for (List<PlaceInfo> mg : middleGroups) {
                if (!mg.isEmpty()) {
                  ids.add(mg.get(0).id());
                }
              }
              if (!endGroup.isEmpty()) {
                ids.add(endGroup.get(0).id());
              }
              return ids;
            });
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var saved = new ArrayList<CoursePlace>();
    when(coursePlaceRepository.save(any(CoursePlace.class)))
        .thenAnswer(
            inv -> {
              saved.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    courseService.createCourse(
        userId,
        petId,
        LocalDate.now(),
        "강남구",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        "종로구",
        new BigDecimal("37.6"),
        new BigDecimal("126.9"),
        1,
        (short) 25,
        (short) 60,
        "맑음");

    assertThat(saved).hasSize(3);
    assertThat(saved.get(0).isFinalPlace()).isFalse();
    assertThat(saved.get(2).isFinalPlace()).isTrue();
  }

  @Test
  void 중간구역이_비어도_있는_장소만으로_코스를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt()))
        .thenAnswer(
            inv -> {
              BigDecimal lat = inv.getArgument(0);
              BigDecimal lng = inv.getArgument(1);
              return List.of(samplePlace("p-" + lng.toPlainString(), "장소", lat, lng));
            });
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.selectAndOrder(
            any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              List<PlaceInfo> startGroup = inv.getArgument(0);
              List<PlaceInfo> endGroup = inv.getArgument(2);
              return List.of(startGroup.get(0).id(), endGroup.get(0).id());
            });
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    var saved = new ArrayList<CoursePlace>();
    when(coursePlaceRepository.save(any(CoursePlace.class)))
        .thenAnswer(
            inv -> {
              saved.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    TravelCourse result =
        courseService.createCourse(
            userId,
            petId,
            LocalDate.now(),
            "강남구",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "종로구",
            new BigDecimal("37.6"),
            new BigDecimal("126.9"),
            4,
            (short) 25,
            (short) 60,
            "맑음");

    assertThat(result).isNotNull();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).isFinalPlace()).isFalse();
    assertThat(saved.get(1).isFinalPlace()).isTrue();
  }

  @Test
  void 타인의_petId로_코스_저장시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(false);

    assertThatThrownBy(
            () ->
                courseService.createCourse(
                    userId,
                    petId,
                    LocalDate.now(),
                    "강남구",
                    new BigDecimal("37.5"),
                    new BigDecimal("127.0"),
                    "종로구",
                    new BigDecimal("37.6"),
                    new BigDecimal("126.9"),
                    4,
                    null,
                    null,
                    null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void 검색결과가_없으면_코스_저장시_예외발생한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));

    assertThatThrownBy(
            () ->
                courseService.createCourse(
                    userId,
                    petId,
                    LocalDate.now(),
                    "강남구",
                    new BigDecimal("37.5"),
                    new BigDecimal("127.0"),
                    "종로구",
                    new BigDecimal("37.6"),
                    new BigDecimal("126.9"),
                    4,
                    null,
                    null,
                    null))
        .isInstanceOf(NoPlacesFoundException.class);
  }

  @Test
  void 코스를_완료한다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    courseService.completeCourse(userId, courseId);

    assertThat(course.isCompleted()).isTrue();
  }

  @Test
  void 이미_완료된_코스_재완료시_멱등처리된다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    course.complete();
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    courseService.completeCourse(userId, courseId);

    assertThat(course.isCompleted()).isTrue();
  }

  @Test
  void 타인_코스_완료시_예외발생한다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(ownerId);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.completeCourse(otherId, courseId))
        .isInstanceOf(CourseNotOwnerException.class);
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
