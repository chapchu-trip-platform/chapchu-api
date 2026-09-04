package com.pettrip.trip.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCourseRequest(
    @NotNull UUID petId,
    @NotNull BigDecimal lat,
    @NotNull BigDecimal lng,
    int radiusMeters,
    @NotNull LocalDate travelDate,
    @NotBlank String startLocation,
    Short temperature,
    Short humidity,
    String weatherStatus) {}
