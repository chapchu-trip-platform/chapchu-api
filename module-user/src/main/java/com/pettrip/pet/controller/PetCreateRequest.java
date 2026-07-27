package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PetCreateRequest(
    @NotBlank String petName,
    @NotNull UUID breedId,
    @NotNull PetSize size,
    @NotNull @Min(0) Integer age) {}
