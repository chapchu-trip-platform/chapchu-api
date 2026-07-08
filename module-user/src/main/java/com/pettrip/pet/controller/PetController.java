package com.pettrip.pet.controller;

import com.pettrip.common.service.TempAuthContext;
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
  public List<PetResponse> listPets() {
    return petService.listPets(TempAuthContext.TEMP_USER_ID).stream()
        .map(PetResponse::from)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PetResponse createPet(@RequestBody @Valid PetCreateRequest request) {
    Pet pet =
        petService.createPet(
            TempAuthContext.TEMP_USER_ID,
            request.breedId(),
            request.petName(),
            request.size(),
            request.age());
    return PetResponse.from(pet);
  }

  @PatchMapping("/{petId}")
  public PetResponse updatePet(
      @PathVariable UUID petId, @RequestBody @Valid PetUpdateRequest request) {
    Pet pet =
        petService.updatePet(
            TempAuthContext.TEMP_USER_ID,
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
    petService.deletePet(TempAuthContext.TEMP_USER_ID, petId);
  }
}
