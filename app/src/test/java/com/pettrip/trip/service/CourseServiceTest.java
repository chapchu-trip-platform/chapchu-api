package com.pettrip.trip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.pet.service.PetNotFoundException;
import com.pettrip.photo.service.PhotoService;
import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import com.pettrip.place.service.PlaceService;
import com.pettrip.recommendation.service.PlaceRagService;
import com.pettrip.recommendation.service.RouteOptimizationService;
import com.pettrip.recommendation.service.SelectedPlace;
import com.pettrip.stamp.service.StampService;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CoursePlaceRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import com.pettrip.trip.service.CourseService.RecommendedPlaceResult;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

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
  @Mock private PhotoService photoService;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;
  @Mock private StampService stampService;

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
        UUID.randomUUID(),
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
            userId,
            petId,
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            5000,
            null,
            null,
            null,
            null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).place().getExternalPlaceId()).isEqualTo("p1");
    assertThat(result.get(0).categoryLabel()).isNotNull();
  }

  @Test
  void 추천은_limit이_있으면_상위_N개만_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);

    List<Place> places =
        List.of(samplePlace("p1", "A"), samplePlace("p2", "B"), samplePlace("p3", "C"));
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(places);
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any(), any(), any(), any(), any()))
        .thenReturn(List.of("p1", "p2", "p3"));

    List<RecommendedPlaceResult> result =
        courseService.recommendPlaces(
            userId,
            petId,
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            5000,
            null,
            null,
            null,
            2);

    assertThat(result).hasSize(2);
  }

  @Test
  void 추천_RAG쿼리에_반려동물_취향이_포함된다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    Pet pet = samplePet(userId);
    pet.replaceActivities(Set.of(new PetActivity("수영")));
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petPolicyRepository.findAllById(any())).thenReturn(List.of());
    when(placeService.searchNearby(any(), any(), anyInt()))
        .thenReturn(List.of(samplePlace("p1", "A")));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    when(placeRagService.rankByReviewSimilarity(any(), queryCaptor.capture()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.optimizeOrder(any(), any(), any(), any(), any()))
        .thenReturn(List.of("p1"));

    courseService.recommendPlaces(
        userId,
        petId,
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        5000,
        null,
        null,
        "맑음",
        null);

    assertThat(queryCaptor.getValue()).contains("수영");
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
                    null,
                    null))
        .isInstanceOf(PetNotFoundException.class);
  }

  private DestinationInput sampleDestination() {
    return new DestinationInput(
        "dest-1",
        "도착장소",
        null,
        "서울시",
        new BigDecimal("37.6"),
        new BigDecimal("126.9"),
        "ALL",
        false,
        false,
        "BOTH",
        null);
  }

  @Test
  void createCourse는_큐레이션_스탑과_고정_도착지를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt()))
        .thenReturn(List.of(samplePlace("p1", "A"), samplePlace("p2", "B")));
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.curateCourse(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of(new SelectedPlace("p1", "물놀이 좋아요"), new SelectedPlace("p2", "산책 코스")));
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    List<CoursePlace> saved = new ArrayList<>();
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
            sampleDestination(),
            (short) 25,
            (short) 60,
            "맑음");

    assertThat(result).isNotNull();
    assertThat(result.getPetId()).isEqualTo(petId);
    assertThat(result.getEndLocation()).isEqualTo("도착장소");
    assertThat(saved).hasSize(3);
    assertThat(saved.get(0).getReason()).isEqualTo("물놀이 좋아요");
    assertThat(saved.get(2).isFinalPlace()).isTrue();
    // 도착지는 places에 안 쌓고 course_place에 비정규화 저장 → external_place_id null, 이름·좌표 세팅
    assertThat(saved.get(2).getExternalPlaceId()).isNull();
    assertThat(saved.get(2).getPlaceName()).isEqualTo("도착장소");
    assertThat(saved.get(2).getLatitude()).isEqualByComparingTo("37.6");
    // 도착지 allowedPetSize="ALL" + SMALL 펫 → 반려견 가능
    assertThat(saved.get(2).getPetAllowed()).isTrue();
  }

  @Test
  void createCourse는_도착지가_반려견_불가면_petAllowed를_false로_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    when(petRepository.existsByIdAndUserId(petId, userId)).thenReturn(true);
    when(petRepository.findById(petId))
        .thenReturn(Optional.of(new Pet(userId, null, "왕", PetSize.LARGE, 5)));
    when(petPolicyRepository.findAllById(any())).thenReturn(List.of());
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.curateCourse(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    List<CoursePlace> saved = new ArrayList<>();
    when(coursePlaceRepository.save(any(CoursePlace.class)))
        .thenAnswer(
            inv -> {
              saved.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    // 도착지 allowedPetSize="SMALL" + LARGE 펫 → 반려견 불가
    DestinationInput smallOnly =
        new DestinationInput(
            "dest-1",
            "소형견 전용 카페",
            null,
            "서울시",
            new BigDecimal("37.6"),
            new BigDecimal("126.9"),
            "SMALL",
            false,
            false,
            "BOTH",
            null);
    courseService.createCourse(
        userId,
        petId,
        LocalDate.now(),
        "강남구",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        smallOnly,
        (short) 25,
        (short) 60,
        "맑음");

    assertThat(saved.get(0).isFinalPlace()).isTrue();
    assertThat(saved.get(0).getPetAllowed()).isFalse();
  }

  @Test
  void createCourse는_도착지를_places에_upsert하지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.curateCourse(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(coursePlaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    courseService.createCourse(
        userId,
        petId,
        LocalDate.now(),
        "강남구",
        new BigDecimal("37.5"),
        new BigDecimal("127.0"),
        sampleDestination(),
        (short) 25,
        (short) 60,
        "맑음");

    verify(placeService, never())
        .upsertPlace(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void createCourse는_후보가_없어도_도착지로_코스를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    mockPetAndPolicy(userId, petId);
    when(placeService.searchNearby(any(), any(), anyInt())).thenReturn(List.of());
    when(placeRagService.rankByReviewSimilarity(any(), any()))
        .thenAnswer(inv -> inv.getArgument(0));
    when(routeOptimizationService.curateCourse(
            any(), anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(List.of());
    when(travelCourseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    List<CoursePlace> saved = new ArrayList<>();
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
            sampleDestination(),
            (short) 25,
            (short) 60,
            "맑음");

    assertThat(result).isNotNull();
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).isFinalPlace()).isTrue();
    assertThat(saved.get(0).getExternalPlaceId()).isNull();
    assertThat(saved.get(0).getPlaceName()).isEqualTo("도착장소");
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
                    sampleDestination(),
                    null,
                    null,
                    null))
        .isInstanceOf(PetNotFoundException.class);
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
  void 코스_완료시_도착지_지역_스탬프를_발급한다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace destination =
        CoursePlace.destination(
            course,
            (short) 2,
            "안동 월영교",
            new BigDecimal("36.55"),
            new BigDecimal("128.77"),
            null,
            "사용자가 선택한 도착지");
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(coursePlaceRepository.findByCourseIdOrderByVisitOrderAsc(courseId))
        .thenReturn(List.of(destination));
    when(placeService.resolveAreaCode(any(), any())).thenReturn((short) 35);

    courseService.completeCourse(userId, courseId);

    assertThat(course.isCompleted()).isTrue();
    verify(stampService).grantForArea(userId, (short) 35);
  }

  @Test
  void 스탬프_발급이_실패해도_완료는_성공한다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace destination =
        CoursePlace.destination(
            course,
            (short) 2,
            "안동 월영교",
            new BigDecimal("36.55"),
            new BigDecimal("128.77"),
            null,
            "사용자가 선택한 도착지");
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(coursePlaceRepository.findByCourseIdOrderByVisitOrderAsc(courseId))
        .thenReturn(List.of(destination));
    when(placeService.resolveAreaCode(any(), any())).thenThrow(new RuntimeException("TourAPI 다운"));

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
  void 코스를_삭제하면_리포지토리에서_제거된다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    courseService.deleteCourse(userId, courseId);

    verify(travelCourseRepository).delete(course);
  }

  @Test
  void 타인_코스_삭제시_예외발생하고_삭제되지_않는다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(ownerId);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThatThrownBy(() -> courseService.deleteCourse(otherId, courseId))
        .isInstanceOf(CourseNotOwnerException.class);
    verify(travelCourseRepository, never()).delete(any(TravelCourse.class));
  }

  @Test
  void 존재하지_않는_코스_삭제시_예외가_발생한다() {
    UUID courseId = UUID.randomUUID();
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> courseService.deleteCourse(UUID.randomUUID(), courseId))
        .isInstanceOf(CourseNotFoundException.class);
    verify(travelCourseRepository, never()).delete(any(TravelCourse.class));
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
  void getCourseReviews는_스탑별_주인리뷰와_사진을_묶어_반환한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace cp1 = new CoursePlace(course, "p1", (short) 1, false);
    CoursePlace cp2 = new CoursePlace(course, "p2", (short) 2, true);
    when(travelCourseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(coursePlaceRepository.findByCourseIdOrderByVisitOrderAsc(courseId))
        .thenReturn(List.of(cp1, cp2));
    when(placeRepository.findAllById(any()))
        .thenReturn(List.of(samplePlace("p1", "공원"), samplePlace("p2", "도착지")));
    when(petPolicyRepository.findAllById(any())).thenReturn(List.of());
    UUID reviewId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(
            List.of(
                new CourseService.CourseReviewRow(
                    cp1.getId(),
                    reviewId,
                    (short) 5,
                    "좋아요",
                    "맑음",
                    LocalDateTime.now(),
                    photoId,
                    "review/u/x.jpg",
                    LocalDate.of(2026, 9, 10))));
    when(photoService.issueDownloadUrl("review/u/x.jpg"))
        .thenReturn(URI.create("https://s3/x").toURL());

    CourseService.CourseReviewsDetail res = courseService.getCourseReviews(userId, courseId);

    assertThat(res.stops()).hasSize(2);
    assertThat(res.stops().get(0).review()).isNotNull();
    assertThat(res.stops().get(0).review().photos()).hasSize(1);
    assertThat(res.stops().get(0).review().photos().get(0).downloadUrl()).contains("https://s3");
    assertThat(res.stops().get(1).review()).isNull();
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
  void 방문_체크인은_더는_스탬프를_발급하지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, false);
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));
    when(placeRepository.findById("place-1")).thenReturn(Optional.of(samplePlace("place-1", "장소")));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    verify(stampService, never()).grantForPlace(any(), any());
  }

  @Test
  void 이미_방문한_장소는_스탬프를_다시_발급하지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, false);
    coursePlace.markVisited();
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    verify(stampService, never()).grantForPlace(any(), any());
  }

  @Test
  void 마지막_장소_체크인해도_코스가_자동완료되지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    TravelCourse course = sampleCourse(userId);
    CoursePlace coursePlace = sampleCoursePlace(course, true);
    when(coursePlaceRepository.findByIdAndCourseUserId(coursePlaceId, userId))
        .thenReturn(Optional.of(coursePlace));
    when(placeRepository.findById("place-1")).thenReturn(Optional.of(samplePlace("place-1", "장소")));

    courseService.visitPlace(userId, coursePlaceId, 37.5, 127.0);

    assertThat(coursePlace.isVisited()).isTrue();
    assertThat(course.isCompleted()).isFalse();
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
