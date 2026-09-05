package com.pettrip.trip.service;

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
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
      CoursePlaceRepository coursePlaceRepository) {
    this.placeService = placeService;
    this.placeRepository = placeRepository;
    this.petPolicyRepository = petPolicyRepository;
    this.petRepository = petRepository;
    this.routeOptimizationService = routeOptimizationService;
    this.placeRagService = placeRagService;
    this.travelCourseRepository = travelCourseRepository;
    this.coursePlaceRepository = coursePlaceRepository;
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
      String weatherStatus) {

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
        .toList();
  }

  @Transactional
  public TravelCourse createCourse(
      UUID userId,
      UUID petId,
      LocalDate travelDate,
      String startLocation,
      BigDecimal startLat,
      BigDecimal startLng,
      String endLocation,
      BigDecimal endLat,
      BigDecimal endLng,
      int intermediateStopCount,
      Short temperature,
      Short humidity,
      String weatherStatus) {

    if (!petRepository.existsByIdAndUserId(petId, userId)) {
      throw new PetNotFoundException();
    }
    Pet pet = petRepository.findById(petId).orElseThrow(PetNotFoundException::new);

    BigDecimal latDelta = endLat.subtract(startLat);
    BigDecimal lngDelta = endLng.subtract(startLng);
    int totalZones = intermediateStopCount + 2;

    Set<String> assigned = new HashSet<>();
    List<Place> startRaw = new ArrayList<>();
    List<List<Place>> middleRaws = new ArrayList<>();
    List<Place> endRaw = new ArrayList<>();

    for (int i = 0; i < totalZones; i++) {
      BigDecimal ratio0 =
          BigDecimal.valueOf(i).divide(BigDecimal.valueOf(totalZones), MathContext.DECIMAL64);
      BigDecimal ratio1 =
          BigDecimal.valueOf(i + 1).divide(BigDecimal.valueOf(totalZones), MathContext.DECIMAL64);

      BigDecimal sliceStartLat = startLat.add(latDelta.multiply(ratio0));
      BigDecimal sliceEndLat = startLat.add(latDelta.multiply(ratio1));
      BigDecimal sliceStartLng = startLng.add(lngDelta.multiply(ratio0));
      BigDecimal sliceEndLng = startLng.add(lngDelta.multiply(ratio1));

      BigDecimal zoneMinLat = sliceStartLat.min(sliceEndLat);
      BigDecimal zoneMaxLat = sliceStartLat.max(sliceEndLat);
      BigDecimal zoneMinLng = sliceStartLng.min(sliceEndLng);
      BigDecimal zoneMaxLng = sliceStartLng.max(sliceEndLng);

      List<Place> zoneRaw = searchInZone(zoneMinLat, zoneMaxLat, zoneMinLng, zoneMaxLng);
      List<Place> zoneGroup = assignGroup(zoneRaw, assigned);

      if (i == 0) {
        startRaw = zoneGroup;
        continue;
      }
      if (i == totalZones - 1) {
        endRaw = zoneGroup;
        continue;
      }
      middleRaws.add(zoneGroup);
    }

    String ragQuery = buildRagQuery(pet, weatherStatus, temperature);
    List<PlaceInfo> startInfos =
        buildGroupInfos(startRaw, PlaceInfo.PlaceGroup.START, pet, ragQuery);
    List<PlaceInfo> endInfos = buildGroupInfos(endRaw, PlaceInfo.PlaceGroup.END, pet, ragQuery);
    List<List<PlaceInfo>> middleInfos =
        middleRaws.stream()
            .map(places -> buildGroupInfos(places, PlaceInfo.PlaceGroup.MIDDLE, pet, ragQuery))
            .toList();

    if (startInfos.isEmpty() || endInfos.isEmpty()) {
      throw new NoPlacesFoundException();
    }

    List<String> orderedIds =
        routeOptimizationService.selectAndOrder(
            startInfos,
            middleInfos,
            endInfos,
            intermediateStopCount,
            startLat,
            startLng,
            endLat,
            endLng,
            toPetSizeLabel(pet.getSize()),
            pet.getAge(),
            weatherStatus,
            temperature);

    TravelCourse course =
        new TravelCourse(
            userId, startLocation, startLat, startLng, endLocation, endLat, endLng, travelDate);
    travelCourseRepository.save(course);

    for (int i = 0; i < orderedIds.size(); i++) {
      boolean isLast = (i == orderedIds.size() - 1);
      String placeId = orderedIds.get(i);
      coursePlaceRepository.save(new CoursePlace(course, placeId, (short) (i + 1), isLast));
    }

    return course;
  }

  private List<Place> searchInZone(
      BigDecimal zoneMinLat, BigDecimal zoneMaxLat, BigDecimal zoneMinLng, BigDecimal zoneMaxLng) {

    BigDecimal centerLat =
        zoneMinLat.add(zoneMaxLat).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    BigDecimal centerLng =
        zoneMinLng.add(zoneMaxLng).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
    int radius =
        (int)
            Math.min(
                haversineMeters(
                    zoneMinLat.doubleValue(),
                    zoneMinLng.doubleValue(),
                    centerLat.doubleValue(),
                    centerLng.doubleValue()),
                20000);
    radius = Math.max(radius, 1000);

    List<Place> result =
        filterByZone(
            placeService.searchNearby(centerLat, centerLng, radius),
            zoneMinLat,
            zoneMaxLat,
            zoneMinLng,
            zoneMaxLng);
    if (!result.isEmpty()) {
      return result;
    }

    int expanded = (int) Math.min(radius * 1.5, 20000);
    return filterByZone(
        placeService.searchNearby(centerLat, centerLng, expanded),
        zoneMinLat,
        zoneMaxLat,
        zoneMinLng,
        zoneMaxLng);
  }

  private List<Place> filterByZone(
      List<Place> places,
      BigDecimal minLat,
      BigDecimal maxLat,
      BigDecimal minLng,
      BigDecimal maxLng) {
    return places.stream()
        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
        .filter(
            p ->
                p.getLatitude().compareTo(minLat) >= 0
                    && p.getLatitude().compareTo(maxLat) <= 0
                    && p.getLongitude().compareTo(minLng) >= 0
                    && p.getLongitude().compareTo(maxLng) <= 0)
        .toList();
  }

  private List<Place> assignGroup(List<Place> places, Set<String> assigned) {
    List<Place> group = new ArrayList<>();
    for (Place p : places) {
      if (assigned.contains(p.getExternalPlaceId())) {
        continue;
      }
      assigned.add(p.getExternalPlaceId());
      group.add(p);
    }
    return group;
  }

  private List<PlaceInfo> buildGroupInfos(
      List<Place> places, PlaceInfo.PlaceGroup group, Pet pet, String ragQuery) {
    List<String> placeIds = places.stream().map(Place::getExternalPlaceId).toList();
    Map<String, PlacePetPolicy> policyMap =
        petPolicyRepository.findAllById(placeIds).stream()
            .collect(Collectors.toMap(PlacePetPolicy::getExternalPlaceId, Function.identity()));
    List<String> filtered = filterByPetSizeWithPolicy(places, pet.getSize(), policyMap);
    List<String> ranked = placeRagService.rankByReviewSimilarity(filtered, ragQuery);
    Map<String, Place> placeMap =
        places.stream().collect(Collectors.toMap(Place::getExternalPlaceId, Function.identity()));
    return ranked.stream()
        .map(placeMap::get)
        .filter(Objects::nonNull)
        .map(p -> toPlaceInfo(p, policyMap.get(p.getExternalPlaceId()), group))
        .toList();
  }

  private List<String> filterByPetSizeWithPolicy(
      List<Place> places, PetSize petSize, Map<String, PlacePetPolicy> policyMap) {
    return places.stream()
        .map(Place::getExternalPlaceId)
        .filter(id -> isPetAllowed(petSize, policyMap.get(id)))
        .toList();
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
    if (policy == null) return false;
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
