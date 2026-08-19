package com.pettrip.trip.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCourseRequest(
    @NotNull BigDecimal lat,
    @NotNull BigDecimal lng,
    int radiusMeters,
    @NotNull LocalDate travelDate,
    @NotBlank String startLocation) {}
