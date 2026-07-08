package com.pettrip.place.controller;

import com.pettrip.place.model.AllowedPetSize;
import com.pettrip.place.model.IndoorOutdoorType;

public record PlacePetPolicyRequest(
    AllowedPetSize allowedPetSize,
    Boolean leashRequired,
    Boolean carrierRequired,
    IndoorOutdoorType indoorOutdoorType,
    Boolean parking,
    String placeCaution) {}
