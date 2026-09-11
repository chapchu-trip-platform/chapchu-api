package com.pettrip.pet.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.pet.model.Pet;
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
    return petService.listPets(userId).stream().map(PetResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PetResponse createPet(
      @CurrentUserId UUID userId, @RequestBody @Valid PetCreateRequest request) {
    Pet pet =
        petService.createPet(
            userId,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age(),
            request.activityIds());
    return PetResponse.from(pet);
  }

  @PatchMapping("/{petId}")
  public PetResponse updatePet(
      @CurrentUserId UUID userId,
      @PathVariable UUID petId,
      @RequestBody @Valid PetUpdateRequest request) {
    Pet pet =
        petService.updatePet(
            userId,
            petId,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age(),
            request.activityIds());
    return PetResponse.from(pet);
  }

  /**
   * 무지개다리를 건넜다고 표시한다. 이 순간부터 그 아이의 앨범이 추억앨범으로 분류된다.
   *
   * <p>되돌리기 어려운 상태 변화라 {@code PATCH /pets/{petId}}와 분리했다.
   */
  @PatchMapping("/{petId}/memorial")
  public PetResponse markMemorial(@CurrentUserId UUID userId, @PathVariable UUID petId) {
    return PetResponse.from(petService.markDie(userId, petId));
  }

  /** 잘못 표시한 경우 되돌린다. */
  @DeleteMapping("/{petId}/memorial")
  public PetResponse restoreMemorial(@CurrentUserId UUID userId, @PathVariable UUID petId) {
    return PetResponse.from(petService.restoreDie(userId, petId));
  }

  @DeleteMapping("/{petId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePet(@CurrentUserId UUID userId, @PathVariable UUID petId) {
    petService.deletePet(userId, petId);
  }
}
