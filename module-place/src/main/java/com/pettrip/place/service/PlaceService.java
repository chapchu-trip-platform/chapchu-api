package com.pettrip.place.service;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.place.repository.PlacePetPolicyRepository;
import com.pettrip.place.repository.PlaceRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceService {

  private final PlaceRepository placeRepository;
  private final PlacePetPolicyRepository petPolicyRepository;

  public PlaceService(
      PlaceRepository placeRepository, PlacePetPolicyRepository petPolicyRepository) {
    this.placeRepository = placeRepository;
    this.petPolicyRepository = petPolicyRepository;
  }

  @Transactional(readOnly = true)
  public Place getPlace(String externalPlaceId) {
    return placeRepository.findById(externalPlaceId).orElseThrow(PlaceNotFoundException::new);
  }

  @Transactional
  public Place upsertPlace(
      String externalPlaceId,
      UUID themeId,
      String placeName,
      String placeImageUrl,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String businessHours,
      String phoneNumber,
      Short rating) {
    return placeRepository
        .findById(externalPlaceId)
        .map(
            existing -> {
              existing.update(
                  themeId,
                  placeName,
                  placeImageUrl,
                  address,
                  latitude,
                  longitude,
                  businessHours,
                  phoneNumber,
                  rating);
              return placeRepository.save(existing);
            })
        .orElseGet(
            () ->
                placeRepository.save(
                    new Place(
                        externalPlaceId,
                        themeId,
                        placeName,
                        placeImageUrl,
                        address,
                        latitude,
                        longitude,
                        businessHours,
                        phoneNumber,
                        rating)));
  }

  @Transactional
  public PlacePetPolicy upsertPetPolicy(
      String externalPlaceId,
      AllowedPetSize allowedPetSize,
      Boolean leashRequired,
      Boolean carrierRequired,
      IndoorOutdoorType indoorOutdoorType,
      Boolean parking,
      String placeCaution) {
    Place place =
        placeRepository.findById(externalPlaceId).orElseThrow(PlaceNotFoundException::new);
    return petPolicyRepository
        .findById(externalPlaceId)
        .map(
            existing -> {
              existing.update(
                  allowedPetSize,
                  leashRequired,
                  carrierRequired,
                  indoorOutdoorType,
                  parking,
                  placeCaution);
              return petPolicyRepository.save(existing);
            })
        .orElseGet(
            () ->
                petPolicyRepository.save(
                    new PlacePetPolicy(
                        place,
                        allowedPetSize,
                        leashRequired,
                        carrierRequired,
                        indoorOutdoorType,
                        parking,
                        placeCaution)));
  }
}
