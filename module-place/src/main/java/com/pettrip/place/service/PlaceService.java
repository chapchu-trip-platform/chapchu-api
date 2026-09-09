package com.pettrip.place.service;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

  private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

  /**
   * 찜 여부는 {@code place_wishlists}를 직접 조회한다.
   *
   * <p>이 테이블은 module-user 소관이지만, module-user가 이미 module-place를 의존하고 있어 반대 방향 의존을 추가하면 순환이 된다. 게시글에서
   * {@code users}·{@code photos}를 직접 조회하는 것과 같은 방식이다.
   */
  private static final String WISHLISTED_SQL =
      """
      SELECT place_id FROM place_wishlists
      WHERE user_id = :userId AND place_id IN (:placeIds)
      """;

  private final PlaceRepository placeRepository;
  private final PlacePetPolicyRepository petPolicyRepository;
  private final TourApiClient tourApiClient;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PlaceService(
      PlaceRepository placeRepository,
      PlacePetPolicyRepository petPolicyRepository,
      TourApiClient tourApiClient,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.placeRepository = placeRepository;
    this.petPolicyRepository = petPolicyRepository;
    this.tourApiClient = tourApiClient;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * 주어진 장소들 중 요청한 사용자가 찜한 것의 id를 돌려준다.
   *
   * <p>{@code GET /places/**}는 공개 엔드포인트라 비로그인 요청이 들어온다. 그 경우 빈 집합을 돌려주고 쿼리를 아예 돌리지 않는다.
   */
  @Transactional(readOnly = true)
  public Set<String> findWishlistedPlaceIds(Optional<UUID> userId, Collection<String> placeIds) {
    if (userId.isEmpty() || placeIds.isEmpty()) {
      return Set.of();
    }
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId.get()).addValue("placeIds", placeIds);
    return Set.copyOf(jdbcTemplate.queryForList(WISHLISTED_SQL, params, String.class));
  }

  @Transactional(readOnly = true)
  public Place getPlace(String externalPlaceId) {
    return placeRepository.findById(externalPlaceId).orElseThrow(PlaceNotFoundException::new);
  }

  @Transactional
  public List<Place> searchNearby(BigDecimal lat, BigDecimal lng, int radiusMeters) {
    try {
      List<TourApiClient.NearbyItem> raw = tourApiClient.fetchNearby(lat, lng, radiusMeters);
      Map<String, TourApiClient.NearbyItem> dedupMap = new LinkedHashMap<>();
      for (TourApiClient.NearbyItem item : raw) {
        dedupMap.putIfAbsent(item.contentId(), item);
      }
      List<TourApiClient.NearbyItem> items = new ArrayList<>(dedupMap.values());
      log.info(
          "TourAPI fetchNearby 결과 {}건 (lat={}, lng={}, radius={})",
          items.size(),
          lat,
          lng,
          radiusMeters);
      List<Place> result = items.stream().map(this::syncPlace).toList();
      log.info("장소 동기화 완료 {}건 / 전체 {}건", result.size(), items.size());
      return result;
    } catch (Exception e) {
      log.error("searchNearby 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
      return List.of();
    }
  }

  private Place syncPlace(TourApiClient.NearbyItem item) {
    Short contentTypeId =
        item.contentTypeId() != null ? Short.parseShort(item.contentTypeId()) : null;

    Place place =
        placeRepository
            .findById(item.contentId())
            .orElseGet(
                () ->
                    new Place(
                        item.contentId(),
                        null,
                        item.title(),
                        item.firstImage(),
                        item.addr1(),
                        item.lat(),
                        item.lng(),
                        null,
                        null,
                        null));

    place.update(
        null,
        item.title(),
        item.firstImage(),
        item.addr1(),
        item.lat(),
        item.lng(),
        null,
        null,
        null,
        contentTypeId);
    Place saved = placeRepository.save(place);

    syncPetPolicy(saved, item.contentId());
    return saved;
  }

  /**
   * FE가 보낸 장소 정보로 Place(+PlacePetPolicy)를 upsert한다. 코스 도착지 확정용.
   *
   * <p>정책은 반드시 저장된(관리 상태) Place 인스턴스로 생성해야 @MapsId 충돌(#202)이 안 난다.
   */
  @Transactional
  public Place upsertPlace(
      String externalPlaceId,
      String placeName,
      String placeImageUrl,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      String indoorOutdoorType,
      String placeCaution) {
    Place place =
        placeRepository
            .findById(externalPlaceId)
            .orElseGet(
                () ->
                    new Place(
                        externalPlaceId,
                        null,
                        placeName,
                        placeImageUrl,
                        address,
                        latitude,
                        longitude,
                        null,
                        null,
                        null));
    place.update(
        null, placeName, placeImageUrl, address, latitude, longitude, null, null, null, null);
    Place saved = placeRepository.save(place);

    AllowedPetSize size = toAllowedPetSize(allowedPetSize);
    IndoorOutdoorType indoor = toIndoorOutdoorType(indoorOutdoorType);
    PlacePetPolicy policy =
        petPolicyRepository
            .findById(externalPlaceId)
            .orElseGet(
                () ->
                    new PlacePetPolicy(
                        saved, size, leashRequired, carrierRequired, indoor, null, placeCaution));
    policy.update(size, leashRequired, carrierRequired, indoor, null, placeCaution);
    petPolicyRepository.save(policy);
    return saved;
  }

  private static AllowedPetSize toAllowedPetSize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return AllowedPetSize.valueOf(value);
  }

  private static IndoorOutdoorType toIndoorOutdoorType(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return IndoorOutdoorType.valueOf(value);
  }

  private void syncPetPolicy(Place place, String contentId) {
    if (petPolicyRepository.existsById(contentId)) return;
    TourApiClient.PetDetailItem detail = tourApiClient.fetchPetDetail(contentId);
    if (detail == null) {
      log.warn("detailPetTour2 응답 없음 contentId={} -> 펫 정책 미저장(필터에서 제외됨)", contentId);
      return;
    }

    petPolicyRepository.save(
        new PlacePetPolicy(
            place,
            parseAllowedPetSize(detail.acmpyPsblCpam()),
            detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("목줄"),
            detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("케이지"),
            parseIndoorOutdoor(detail.acmpyTypeCd()),
            null,
            detail.etcAcmpyInfo()));
  }

  private AllowedPetSize parseAllowedPetSize(String acmpyPsblCpam) {
    if (acmpyPsblCpam == null) return AllowedPetSize.ALL;
    if (acmpyPsblCpam.contains("소형")) return AllowedPetSize.SMALL;
    if (acmpyPsblCpam.contains("중형")) return AllowedPetSize.MEDIUM;
    if (acmpyPsblCpam.contains("대형")) return AllowedPetSize.LARGE;
    return AllowedPetSize.ALL;
  }

  private IndoorOutdoorType parseIndoorOutdoor(String acmpyTypeCd) {
    if (acmpyTypeCd == null) return IndoorOutdoorType.BOTH;
    if (acmpyTypeCd.contains("실내") && !acmpyTypeCd.contains("실외")) return IndoorOutdoorType.INDOOR;
    if (acmpyTypeCd.contains("실외") && !acmpyTypeCd.contains("실내")) return IndoorOutdoorType.OUTDOOR;
    return IndoorOutdoorType.BOTH;
  }
}
