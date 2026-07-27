package com.pettrip.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

  @Mock private PetRepository petRepository;
  @Mock private BreedRepository breedRepository;

  private PetService petService;

  @BeforeEach
  void setUp() {
    petService = new PetService(petRepository, breedRepository);
  }

  @Test
  void listPets는_레포지토리에_위임한다() {
    UUID userId = UUID.randomUUID();
    when(petRepository.findByUserId(userId)).thenReturn(List.of());

    petService.listPets(userId);

    verify(petRepository).findByUserId(userId);
  }

  @Test
  void createPet는_견종이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID breedId = UUID.randomUUID();
    when(breedRepository.findById(breedId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3))
        .isInstanceOf(BreedNotFoundException.class);
  }

  @Test
  void createPet는_견종이_있으면_반려견을_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID breedId = UUID.randomUUID();
    Breed breed = new Breed("골든리트리버");
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(breed));
    when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Pet result = petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3);

    assertThat(result.getPetName()).isEqualTo("초코");
    assertThat(result.getBreed()).isEqualTo(breed);
  }

  @Test
  void updatePet는_소유자가_아니면_예외를_던진다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(ownerId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

    assertThatThrownBy(() -> petService.updatePet(otherId, petId, null, "루이", null, null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void deletePet는_소유한_반려견을_삭제한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

    petService.deletePet(userId, petId);

    verify(petRepository).delete(pet);
  }
}
