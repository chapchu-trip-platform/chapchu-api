package com.pettrip.pet.controller;

import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.service.PetDetail;
import com.pettrip.pet.service.PetPhotoView;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * @param profilePhoto 프로필 사진. 없으면 null. 기본 이미지는 프론트가 처리한다
 * @param backgroundPhoto 프로필 배경화면. 없으면 null
 */
public record PetResponse(
    UUID id,
    String petName,
    Integer breedId,
    String breedName,
    PetSize size,
    Integer age,
    boolean isDie,
    PetPhotoView profilePhoto,
    PetPhotoView backgroundPhoto,
    List<PetActivityResponse> activities,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static PetResponse of(PetDetail detail) {
    Pet pet = detail.pet();
    return new PetResponse(
        pet.getId(),
        pet.getPetName(),
        pet.getBreed().getId(),
        pet.getBreed().getBreedName(),
        pet.getSize(),
        pet.getAge(),
        pet.isDie(),
        detail.profilePhoto(),
        detail.backgroundPhoto(),
        pet.getPreferredActivities().stream()
            .map(PetActivityResponse::from)
            .sorted(Comparator.comparing(PetActivityResponse::name))
            .toList(),
        pet.getCreatedAt(),
        pet.getUpdatedAt());
  }
}
