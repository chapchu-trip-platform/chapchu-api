package com.pettrip.pet.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.pet.service.PetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pets")
public class PetController {

  private final PetService petService;

  public PetController(PetService petService) {
    this.petService = petService;
  }

  @GetMapping
  public List<PetResponse> listPets(@CurrentUserId UUID userId) {
    return petService.listPets(userId).stream().map(PetResponse::of).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PetResponse createPet(
      @CurrentUserId UUID userId, @RequestBody @Valid PetCreateRequest request) {
    return PetResponse.of(
        petService.createPet(
            userId,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age(),
            request.activityIds()));
  }

  @PatchMapping("/{petId}")
  public PetResponse updatePet(
      @CurrentUserId UUID userId,
      @PathVariable UUID petId,
      @RequestBody @Valid PetUpdateRequest request) {
    return PetResponse.of(
        petService.updatePet(
            userId,
            petId,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age(),
            request.isDie(),
            request.activityIds()));
  }

  /** 프로필 사진 연결·해제. {@code photoId}를 null로 보내면 사진을 뗀다. */
  @PatchMapping("/{petId}/photo")
  public PetResponse updateProfilePhoto(
      @CurrentUserId UUID userId,
      @PathVariable UUID petId,
      @RequestBody PetPhotoUpdateRequest request) {
    return PetResponse.of(petService.updateProfilePhoto(userId, petId, request.photoId()));
  }

  /** 프로필 배경화면 연결·해제. 사진은 프로필과 같은 경로(type=PROFILE)에 올린다. */
  @PatchMapping("/{petId}/background")
  public PetResponse updateBackgroundPhoto(
      @CurrentUserId UUID userId,
      @PathVariable UUID petId,
      @RequestBody PetPhotoUpdateRequest request) {
    return PetResponse.of(petService.updateBackgroundPhoto(userId, petId, request.photoId()));
  }

  @DeleteMapping("/{petId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePet(@CurrentUserId UUID userId, @PathVariable UUID petId) {
    petService.deletePet(userId, petId);
  }
}
