package com.pettrip.place.service;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

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
    List<TourApiClient.NearbyItem> items = tourApiClient.fetchNearby(lat, lng, radiusMeters);
    return items.stream().map(item -> syncPlace(item)).toList();
  }

  private Place syncPlace(TourApiClient.NearbyItem item) {
    Place place =
        placeRepository
            .findById(item.contentId())
            .map(
                existing -> {
                  existing.update(
                      null,
                      item.title(),
                      item.firstImage(),
                      item.addr1(),
                      item.lat(),
                      item.lng(),
                      null,
                      null,
                      null);
                  return placeRepository.save(existing);
                })
            .orElseGet(
                () ->
                    placeRepository.save(
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
                            null)));

    syncPetPolicy(place, item.contentId());
    return place;
  }

  private void syncPetPolicy(Place place, String contentId) {
    TourApiClient.PetDetailItem detail = tourApiClient.fetchPetDetail(contentId);
    if (detail == null) return;

    petPolicyRepository
        .findById(contentId)
        .ifPresentOrElse(
            existing -> {
              existing.update(
                  parseAllowedPetSize(detail.acmpyTypeCd()),
                  detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("목줄"),
                  detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("케이지"),
                  parseIndoorOutdoor(detail.acmpyTypeCd()),
                  detail.relaPrkge() != null && !detail.relaPrkge().isBlank(),
                  detail.etcAcmpyInfo());
              petPolicyRepository.save(existing);
            },
            () ->
                petPolicyRepository.save(
                    new PlacePetPolicy(
                        place,
                        parseAllowedPetSize(detail.acmpyTypeCd()),
                        detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("목줄"),
                        detail.acmpyNeedMtr() != null && detail.acmpyNeedMtr().contains("케이지"),
                        parseIndoorOutdoor(detail.acmpyTypeCd()),
                        detail.relaPrkge() != null && !detail.relaPrkge().isBlank(),
                        detail.etcAcmpyInfo())));
  }

  private AllowedPetSize parseAllowedPetSize(String acmpyTypeCd) {
    if (acmpyTypeCd == null) return AllowedPetSize.ALL;
    if (acmpyTypeCd.contains("소형")) return AllowedPetSize.SMALL;
    if (acmpyTypeCd.contains("중형")) return AllowedPetSize.MEDIUM;
    if (acmpyTypeCd.contains("대형")) return AllowedPetSize.LARGE;
    return AllowedPetSize.ALL;
  }

  private IndoorOutdoorType parseIndoorOutdoor(String acmpyTypeCd) {
    if (acmpyTypeCd == null) return IndoorOutdoorType.BOTH;
    if (acmpyTypeCd.contains("실내")) return IndoorOutdoorType.INDOOR;
    if (acmpyTypeCd.contains("실외")) return IndoorOutdoorType.OUTDOOR;
    return IndoorOutdoorType.BOTH;
  }
}
