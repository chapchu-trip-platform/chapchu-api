package com.pettrip.place.controller;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

public record PlaceRegisterRequest(
    @NotBlank String externalPlaceId,
    UUID themeId,
    String placeName,
    String placeImageUrl,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String businessHours,
    String phoneNumber,
    Short rating) {}
