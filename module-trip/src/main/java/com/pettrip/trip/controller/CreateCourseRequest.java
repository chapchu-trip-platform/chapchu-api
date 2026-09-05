package com.pettrip.trip.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCourseRequest(
    @NotNull UUID petId,
    @NotNull LocalDate travelDate,
    @NotBlank String startLocation,
    @NotNull BigDecimal startLat,
    @NotNull BigDecimal startLng,
    @NotBlank String endLocation,
    @NotNull BigDecimal endLat,
    @NotNull BigDecimal endLng,
    @Min(0) int intermediateStopCount,
    Short temperature,
    Short humidity,
    String weatherStatus) {}
