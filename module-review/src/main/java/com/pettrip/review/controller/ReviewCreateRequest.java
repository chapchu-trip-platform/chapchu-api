package com.pettrip.review.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ReviewCreateRequest(
    @NotBlank String placeId,
    @NotNull UUID petId,
    @NotNull @Min(1) @Max(5) Short rating,
    @NotBlank String contents,
    @Size(max = 20) String weather,
    UUID coursePlaceId) {}
