package com.pettrip.pet.controller;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @param isDie 무지개다리를 건넜는지. true면 이 아이의 앨범은 추억앨범으로 분류된다
 */
public record PetResponse(
    UUID id,
    String petName,
    Integer breedId,
    String breedName,
    PetSize size,
    Integer age,
    boolean isDie,
    List<PetActivityResponse> activities,
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
        pet.isDie(),
        pet.getPreferredActivities().stream()
            .map(PetActivityResponse::from)
            .sorted(Comparator.comparing(PetActivityResponse::name))
            .toList(),
        pet.getCreatedAt(),
        pet.getUpdatedAt());
  }
}
