package com.pettrip.place.service;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

  private static final Logger log = LoggerFactory.getLogger(PlaceService.class);

  private final PlaceRepository placeRepository;
  private final PlacePetPolicyRepository petPolicyRepository;
  private final TourApiClient tourApiClient;

  public PlaceService(
      PlaceRepository placeRepository,
      PlacePetPolicyRepository petPolicyRepository,
      TourApiClient tourApiClient) {
    this.placeRepository = placeRepository;
    this.petPolicyRepository = petPolicyRepository;
    this.tourApiClient = tourApiClient;
  }

  @Transactional(readOnly = true)
  public Place getPlace(String externalPlaceId) {
    return placeRepository.findById(externalPlaceId).orElseThrow(PlaceNotFoundException::new);
  }

  @Transactional
  public List<Place> searchNearby(BigDecimal lat, BigDecimal lng, int radiusMeters) {
    try {
      List<TourApiClient.NearbyItem> items = tourApiClient.fetchNearby(lat, lng, radiusMeters);
      log.info(
          "TourAPI fetchNearby 결과 {}건 (lat={}, lng={}, radius={})",
          items.size(),
          lat,
          lng,
          radiusMeters);
      List<Place> result =
          items.stream()
              .map(this::syncPlace)
              .filter(place -> petPolicyRepository.existsById(place.getExternalPlaceId()))
              .toList();
      log.info("펫 정책 필터 통과 {}건 / 전체 {}건", result.size(), items.size());
      return result;
    } catch (Exception e) {
      log.error("searchNearby 실패 {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
      return List.of();
    }
  }

  private Place syncPlace(TourApiClient.NearbyItem item) {
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
        null);
    placeRepository.save(place);

    syncPetPolicy(place, item.contentId());
    return place;
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
