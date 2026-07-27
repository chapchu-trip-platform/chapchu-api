package com.pettrip.place.controller;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;
import com.pettrip.place.model.PlacePetPolicy;

public record PlacePetPolicyResponse(
    AllowedPetSize allowedPetSize,
    Boolean leashRequired,
    Boolean carrierRequired,
    IndoorOutdoorType indoorOutdoorType,
    Boolean parking,
    String placeCaution) {

  public static PlacePetPolicyResponse from(PlacePetPolicy policy) {
    return new PlacePetPolicyResponse(
        policy.getAllowedPetSize(),
        policy.getLeashRequired(),
        policy.getCarrierRequired(),
        policy.getIndoorOutdoorType(),
        policy.getParking(),
        policy.getPlaceCaution());
  }
}
