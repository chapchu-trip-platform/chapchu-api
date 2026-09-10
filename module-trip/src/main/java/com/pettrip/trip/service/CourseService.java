package com.pettrip.trip.service;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetActivity;
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
import com.pettrip.recommendation.service.SelectedPlace;
import com.pettrip.stamp.service.StampService;
import com.pettrip.trip.model.CoursePlace;
import com.pettrip.trip.model.TravelCourse;
import com.pettrip.trip.repository.CoursePlaceRepository;
import com.pettrip.trip.repository.TravelCourseRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
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
  private final PlacePetPolicyRepository petPolicyRepository;
  private final PetRepository petRepository;
  private final RouteOptimizationService routeOptimizationService;
  private final PlaceRagService placeRagService;
  private final StampService stampService;
  private final TravelCourseRepository travelCourseRepository;
  private final CoursePlaceRepository coursePlaceRepository;

  public CourseService(
      PlaceService placeService,
      PlaceRepository placeRepository,
      PlacePetPolicyRepository petPolicyRepository,
      PetRepository petRepository,
      RouteOptimizationService routeOptimizationService,
      PlaceRagService placeRagService,
      TravelCourseRepository travelCourseRepository,
      CoursePlaceRepository coursePlaceRepository,
      StampService stampService) {
    this.placeService = placeService;
    this.placeRepository = placeRepository;
    this.petPolicyRepository = petPolicyRepository;
    this.petRepository = petRepository;
    this.routeOptimizationService = routeOptimizationService;
    this.placeRagService = placeRagService;
    this.travelCourseRepository = travelCourseRepository;
    this.coursePlaceRepository = coursePlaceRepository;
    this.stampService = stampService;
  }

  @Transactional(readOnly = true)
  public List<RecommendedPlaceResult> recommendPlaces(
      UUID userId,
      UUID petId,
      BigDecimal lat,
      BigDecimal lng,
      int radiusMeters,
      Short temperature,
      Short humidity,
      String weatherStatus,
      Integer limit) {

    if (!petRepository.existsByIdAndUserId(petId, userId)) {
      throw new PetNotFoundException();
    }
    Pet pet = petRepository.findById(petId).orElseThrow(PetNotFoundException::new);

    List<Place> places = placeService.searchNearby(lat, lng, radiusMeters);

    Map<String, Place> placeMap =
        places.stream().collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity()));

    List<String> policyFilteredIds = filterByPetSize(places, pet.getSize());

    String ragQuery = buildRagQuery(pet, weatherStatus, temperature);
    List<String> ragRankedIds = placeRagService.rankByReviewSimilarity(policyFilteredIds, ragQuery);

    Map<String, PlacePetPolicy> policyMap =
        petPolicyRepository.findAllById(ragRankedIds).stream()
            .collect(Collectors.toMap(PlacePetPolicy::getExternalPlaceId, Function.identity()));

    List<PlaceInfo> placeInfos =
        ragRankedIds.stream()
            .map(placeMap::get)
            .filter(Objects::nonNull)
            .map(p -> toPlaceInfo(p, policyMap.get(p.getExternalPlaceId())))
            .toList();

    List<String> orderedIds =
        routeOptimizationService.optimizeOrder(
            placeInfos, toPetSizeLabel(pet.getSize()), pet.getAge(), weatherStatus, temperature);
    if (orderedIds.isEmpty()) {
      throw new NoPlacesFoundException();
    }

    return orderedIds.stream()
        .map(placeMap::get)
        .filter(Objects::nonNull)
        .map(p -> toRecommendedResult(p, policyMap.get(p.getExternalPlaceId())))
        .limit(resolveLimit(limit))
        .toList();
  }

  /** limit이 없거나 0 이하면 제한 없음(전체). 있으면 상위 limit개로 자른다. */
  private static int resolveLimit(Integer limit) {
    if (limit == null || limit <= 0) {
      return Integer.MAX_VALUE;
    }
    return limit;
  }

  @Transactional
  public TravelCourse createCourse(
      UUID userId,
      UUID petId,
      LocalDate travelDate,
      String startLocation,
      BigDecimal startLat,
      BigDecimal startLng,
      DestinationInput destination,
      Short temperature,
      Short humidity,
      String weatherStatus) {

    if (!petRepository.existsByIdAndUserId(petId, userId)) {
      throw new PetNotFoundException();
    }
    Pet pet = petRepository.findById(petId).orElseThrow(PetNotFoundException::new);

    // 사용자가 고른 도착지를 Place로 upsert(고정 도착 스탑). recommend가 readOnly라 DB에 없을 수 있어 여기서 확정 저장.
    placeService.upsertPlace(
        destination.externalPlaceId(),
        destination.placeName(),
        destination.placeImageUrl(),
        destination.address(),
        destination.latitude(),
        destination.longitude(),
        destination.allowedPetSize(),
        destination.leashRequired(),
        destination.carrierRequired(),
        destination.indoorOutdoorType(),
        destination.placeCaution());

    BigDecimal destLat = destination.latitude();
    BigDecimal destLng = destination.longitude();
    double distance =
        haversineMeters(
            startLat.doubleValue(),
            startLng.doubleValue(),
            destLat.doubleValue(),
            destLng.doubleValue());
    int maxStops =
        computeMaxStops(distance, pet.getSize(), pet.getAge(), weatherStatus, temperature);

    // 출발~도착 사이 후보 풀 수집(도착지 제외) → 크기 필터 → 취향 RAG 랭킹.
    List<Place> pool =
        gatherCandidates(startLat, startLng, destLat, destLng, destination.externalPlaceId());
    Map<String, Place> poolMap =
        pool.stream()
            .collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity(), (a, b) -> a));
    String ragQuery = buildRagQuery(pet, weatherStatus, temperature);
    List<String> filteredIds = filterByPetSize(pool, pet.getSize());
    List<String> rankedIds = placeRagService.rankByReviewSimilarity(filteredIds, ragQuery);
    Map<String, PlacePetPolicy> policyMap =
        petPolicyRepository.findAllById(rankedIds).stream()
            .collect(Collectors.toMap(PlacePetPolicy::getExternalPlaceId, Function.identity()));
    List<PlaceInfo> poolInfos =
        rankedIds.stream()
            .map(poolMap::get)
            .filter(Objects::nonNull)
            .limit(30)
            .map(
                p ->
                    toPlaceInfo(
                        p, policyMap.get(p.getExternalPlaceId()), PlaceInfo.PlaceGroup.MIDDLE))
            .toList();

    List<String> activities =
        pet.getPreferredActivities().stream().map(PetActivity::getActivityName).toList();
    List<SelectedPlace> curated =
        routeOptimizationService.curateCourse(
            poolInfos,
            maxStops,
            startLat,
            startLng,
            destination.placeName(),
            destLat,
            destLng,
            toPetSizeLabel(pet.getSize()),
            pet.getAge(),
            activities,
            weatherStatus,
            temperature);

    TravelCourse course =
        new TravelCourse(
            userId,
            startLocation,
            startLat,
            startLng,
            destination.placeName(),
            destLat,
            destLng,
            travelDate);
    travelCourseRepository.save(course);

    short order = 1;
    for (SelectedPlace stop : curated) {
      coursePlaceRepository.save(new CoursePlace(course, stop.id(), order, false, stop.reason()));
      order++;
    }
    coursePlaceRepository.save(
        new CoursePlace(course, destination.externalPlaceId(), order, true, "사용자가 선택한 도착지"));
    return course;
  }

  /** 출발~도착 중점 반경으로 후보를 모은다(도착지 자신 제외). bbox 대신 원 하나로 회랑을 덮는다. */
  private List<Place> gatherCandidates(
      BigDecimal startLat,
      BigDecimal startLng,
      BigDecimal destLat,
      BigDecimal destLng,
      String excludeId) {
    BigDecimal midLat = startLat.add(destLat).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    BigDecimal midLng = startLng.add(destLng).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    double d =
        haversineMeters(
            startLat.doubleValue(),
            startLng.doubleValue(),
            destLat.doubleValue(),
            destLng.doubleValue());
    int radius = (int) Math.min(Math.max(d / 2 + 2000, 2000), 20000);
    return placeService.searchNearby(midLat, midLng, radius).stream()
        .filter(p -> !p.getExternalPlaceId().equals(excludeId))
        .toList();
  }

  /** 중간 스탑 상한 N = 거리 base + 체력 보정 + 날씨 보정, 1~3 clamp. (docs/decisions/043) */
  private int computeMaxStops(
      double distanceMeters, PetSize size, Integer age, String weather, Short temperature) {
    return clampStops(
        distanceBase(distanceMeters)
            + petAdjustment(size, age)
            + weatherAdjustment(weather, temperature));
  }

  private int distanceBase(double meters) {
    if (meters < 4000) {
      return 1;
    }
    if (meters < 9000) {
      return 2;
    }
    return 3;
  }

  private int petAdjustment(PetSize size, Integer age) {
    if (size == PetSize.SMALL) {
      return -1;
    }
    if (age != null && age >= 8) {
      return -1;
    }
    return 0;
  }

  private int weatherAdjustment(String weather, Short temperature) {
    if (temperature != null && temperature >= 30) {
      return -1;
    }
    if (weather != null && (weather.contains("비") || weather.contains("눈"))) {
      return -1;
    }
    return 0;
  }

  private int clampStops(int n) {
    if (n < 1) {
      return 1;
    }
    if (n > 3) {
      return 3;
    }
    return n;
  }

  @Transactional
  public void completeCourse(UUID userId, UUID courseId) {
    TravelCourse course =
        travelCourseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
    if (!userId.equals(course.getUserId())) {
      throw new CourseNotOwnerException();
    }
    if (course.isCompleted()) {
      return;
    }
    course.complete();
  }

  private List<String> filterByPetSize(List<Place> places, PetSize petSize) {
    List<String> placeIds = places.stream().map(Place::getExternalPlaceId).toList();
    Map<String, PlacePetPolicy> policyMap =
        petPolicyRepository.findAllById(placeIds).stream()
            .collect(Collectors.toMap(PlacePetPolicy::getExternalPlaceId, Function.identity()));

    return places.stream()
        .filter(p -> isPetAllowed(petSize, policyMap.get(p.getExternalPlaceId())))
        .map(Place::getExternalPlaceId)
        .toList();
  }

  private boolean isPetAllowed(PetSize petSize, PlacePetPolicy policy) {
    if (policy == null) return true;
    AllowedPetSize allowed = policy.getAllowedPetSize();
    if (allowed == null || allowed == AllowedPetSize.ALL) return true;
    return switch (petSize) {
      case SMALL -> true;
      case MEDIUM -> allowed == AllowedPetSize.MEDIUM || allowed == AllowedPetSize.LARGE;
      case LARGE -> allowed == AllowedPetSize.LARGE;
    };
  }

  private PlaceInfo toPlaceInfo(Place p, PlacePetPolicy policy, PlaceInfo.PlaceGroup group) {
    return new PlaceInfo(
        p.getExternalPlaceId(),
        p.getPlaceName(),
        p.getAddress(),
        p.getLatitude(),
        p.getLongitude(),
        toCategoryLabel(p.getContentTypeId()),
        toIndoorOutdoor(policy),
        group);
  }

  private PlaceInfo toPlaceInfo(Place p, PlacePetPolicy policy) {
    return toPlaceInfo(p, policy, PlaceInfo.PlaceGroup.MIDDLE);
  }

  private RecommendedPlaceResult toRecommendedResult(Place p, PlacePetPolicy policy) {
    return new RecommendedPlaceResult(
        p, policy, toCategoryLabel(p.getContentTypeId()), toIndoorOutdoor(policy));
  }

  private String toIndoorOutdoor(PlacePetPolicy policy) {
    if (policy == null || policy.getIndoorOutdoorType() == null) return "BOTH";
    return policy.getIndoorOutdoorType().name();
  }

  private String toCategoryLabel(Short contentTypeId) {
    if (contentTypeId == null) return "기타";
    return switch (contentTypeId) {
      case 12 -> "관광지";
      case 14 -> "문화시설";
      case 15 -> "행사";
      case 25 -> "여행코스";
      case 28 -> "레포츠";
      case 32 -> "숙박";
      case 38 -> "쇼핑";
      case 39 -> "음식점";
      default -> "기타";
    };
  }

  private String toPetSizeLabel(PetSize size) {
    if (size == null) return null;
    return switch (size) {
      case SMALL -> "소형";
      case MEDIUM -> "중형";
      case LARGE -> "대형";
    };
  }

  private String buildRagQuery(Pet pet, String weatherStatus, Short temperature) {
    StringBuilder sb = new StringBuilder();
    if (weatherStatus != null) {
      sb.append(weatherStatus).append(" 날씨에 ");
    }
    if (temperature != null) {
      sb.append("기온 ").append(temperature).append("도, ");
    }
    if (pet.getSize() != null) {
      sb.append(pet.getSize().name().toLowerCase()).append("견 ");
    }
    if (pet.getAge() != null) {
      sb.append(pet.getAge()).append("살과 함께 ");
    }
    String activities =
        pet.getPreferredActivities().stream()
            .map(PetActivity::getActivityName)
            .collect(Collectors.joining(", "));
    if (!activities.isBlank()) {
      sb.append(activities).append(" 활동을 즐기기 좋은 ");
    }
    sb.append("반려동물 동반 즐거운 여행 좋은 장소");
    return sb.toString();
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

    Map<String, PlacePetPolicy> policyMap =
        petPolicyRepository.findAllById(placeIds).stream()
            .collect(Collectors.toMap(PlacePetPolicy::getExternalPlaceId, Function.identity()));

    return new TravelCourseDetail(course, coursePlaces, placeMap, policyMap);
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
    stampService.grantForPlace(userId, coursePlace.getExternalPlaceId());
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

  public record RecommendedPlaceResult(
      Place place, PlacePetPolicy policy, String categoryLabel, String indoorOutdoorType) {}

  public record TravelCourseDetail(
      TravelCourse course,
      List<CoursePlace> coursePlaces,
      Map<String, Place> placeMap,
      Map<String, PlacePetPolicy> policyMap) {}
}
