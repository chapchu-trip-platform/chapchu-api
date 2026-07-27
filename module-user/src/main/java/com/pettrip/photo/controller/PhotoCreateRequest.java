package com.pettrip.photo.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PhotoCreateRequest(
    @NotNull UUID coursePlaceId, @NotBlank String photoKey, LocalDate takenAt) {}
