package com.pettrip.pet.controller;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import java.time.LocalDateTime;
import java.util.UUID;

public record PetResponse(
    UUID id,
    String petName,
    UUID breedId,
    String breedName,
    PetSize size,
    Integer age,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static PetResponse from(Pet pet) {
    return new PetResponse(
        pet.getId(),
        pet.getPetName(),
        pet.getBreed().getId(),
        pet.getBreed().getBreedName(),
        pet.getSize(),
        pet.getAge(),
        pet.getCreatedAt(),
        pet.getUpdatedAt());
  }
}
