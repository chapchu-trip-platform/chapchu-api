package com.pettrip.place.controller;

import com.pettrip.place.model.Place;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @param wishlisted 요청한 사용자가 찜했는지. 비로그인 요청이면 항상 false다
 */
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
    boolean wishlisted,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static PlaceResponse from(Place place, boolean wishlisted) {
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
        wishlisted,
        place.getCreatedAt(),
        place.getUpdatedAt());
  }
}
