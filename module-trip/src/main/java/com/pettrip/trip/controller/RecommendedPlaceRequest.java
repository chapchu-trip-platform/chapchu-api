package com.pettrip.trip.controller;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RecommendedPlaceRequest(
    @NotNull UUID petId,
    @NotNull BigDecimal lat,
    @NotNull BigDecimal lng,
    int radiusMeters,
    Short temperature,
    Short humidity,
    String weatherStatus) {}
