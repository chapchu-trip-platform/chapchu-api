package com.pettrip.trip.controller;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record WeatherRecordRequest(
    @NotNull LocalDate weatherDate,
    @NotNull Short temperature,
    @NotNull Short humidity,
    @NotNull String weatherStatus,
    String weatherCaution) {}
