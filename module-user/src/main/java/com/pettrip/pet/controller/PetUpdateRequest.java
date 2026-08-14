package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.constraints.Min;

public record PetUpdateRequest(
    String petName, Integer breedId, PetSize size, @Min(0) Integer age) {}
