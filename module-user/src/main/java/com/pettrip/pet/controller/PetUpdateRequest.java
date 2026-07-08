package com.pettrip.pet.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.constraints.Min;
import java.util.UUID;

public record PetUpdateRequest(String petName, UUID breedId, PetSize size, @Min(0) Integer age) {}
