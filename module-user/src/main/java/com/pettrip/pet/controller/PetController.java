package com.pettrip.pet.controller;

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

  // docs/decisions/011 참고: 실제 인증(OAuth2 Resource Server) 붙기 전까지 쓰는 임시 고정 유저.
  // auth 도메인 구현 시 @AuthenticationPrincipal 기반 추출로 교체 필요.
  private static final UUID TEMP_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  private final PetService petService;

  public PetController(PetService petService) {
    this.petService = petService;
  }

  @GetMapping
  public List<PetResponse> listPets() {
    return petService.listPets(TEMP_USER_ID).stream().map(PetResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PetResponse createPet(@RequestBody @Valid PetCreateRequest request) {
    Pet pet =
        petService.createPet(
            TEMP_USER_ID, request.breedId(), request.petName(), request.size(), request.age());
    return PetResponse.from(pet);
  }

  @PatchMapping("/{petId}")
  public PetResponse updatePet(
      @PathVariable UUID petId, @RequestBody @Valid PetUpdateRequest request) {
    Pet pet =
        petService.updatePet(
            TEMP_USER_ID,
            petId,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age());
    return PetResponse.from(pet);
  }

  @DeleteMapping("/{petId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePet(@PathVariable UUID petId) {
    petService.deletePet(TEMP_USER_ID, petId);
  }
}
