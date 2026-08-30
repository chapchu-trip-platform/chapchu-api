package com.pettrip.trip.controller;

import jakarta.validation.constraints.NotNull;

public record VisitRequest(@NotNull Double lat, @NotNull Double lng) {}
