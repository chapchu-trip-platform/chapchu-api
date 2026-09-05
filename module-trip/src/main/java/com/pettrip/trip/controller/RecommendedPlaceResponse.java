package com.pettrip.trip.controller;

import com.pettrip.place.model.Place;
import com.pettrip.place.model.PlacePetPolicy;
import com.pettrip.trip.service.CourseService.RecommendedPlaceResult;
import java.math.BigDecimal;

public record RecommendedPlaceResponse(
    String externalPlaceId,
    String placeName,
    String placeImageUrl,
    BigDecimal latitude,
    BigDecimal longitude,
    String address,
    String categoryLabel,
    String indoorOutdoorType,
    String allowedPetSize,
    Boolean leashRequired,
    Boolean carrierRequired,
    String placeCaution) {

  public static RecommendedPlaceResponse from(RecommendedPlaceResult result) {
    Place place = result.place();
    PlacePetPolicy policy = result.policy();
    return new RecommendedPlaceResponse(
        place.getExternalPlaceId(),
        place.getPlaceName(),
        place.getPlaceImageUrl(),
        place.getLatitude(),
        place.getLongitude(),
        place.getAddress(),
        result.categoryLabel(),
        result.indoorOutdoorType(),
        allowedPetSize(policy),
        leashRequired(policy),
        carrierRequired(policy),
        placeCaution(policy));
  }

  private static String allowedPetSize(PlacePetPolicy policy) {
    if (policy == null || policy.getAllowedPetSize() == null) return null;
    return policy.getAllowedPetSize().name();
  }

  private static Boolean leashRequired(PlacePetPolicy policy) {
    if (policy == null) return null;
    return policy.getLeashRequired();
  }

  private static Boolean carrierRequired(PlacePetPolicy policy) {
    if (policy == null) return null;
    return policy.getCarrierRequired();
  }

  private static String placeCaution(PlacePetPolicy policy) {
    if (policy == null) return null;
    return policy.getPlaceCaution();
  }
}
