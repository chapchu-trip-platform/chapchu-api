package com.pettrip.place.controller;

import com.pettrip.place.model.Place;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PlaceResponse(
    String externalPlaceId,
    UUID themeId,
    String placeName,
    String placeImageUrl,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String businessHours,
    String phoneNumber,
    Short rating,
    Integer reviewNum,
    Integer visitNum,
    PlacePetPolicyResponse petPolicy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static PlaceResponse from(Place place) {
    return new PlaceResponse(
        place.getExternalPlaceId(),
        place.getThemeId(),
        place.getPlaceName(),
        place.getPlaceImageUrl(),
        place.getAddress(),
        place.getLatitude(),
        place.getLongitude(),
        place.getBusinessHours(),
        place.getPhoneNumber(),
        place.getRating(),
        place.getReviewNum(),
        place.getVisitNum(),
        place.getPetPolicy() != null ? PlacePetPolicyResponse.from(place.getPetPolicy()) : null,
        place.getCreatedAt(),
        place.getUpdatedAt());
  }
}
