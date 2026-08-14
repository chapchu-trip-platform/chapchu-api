package com.pettrip.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetActivityRepository;
import com.pettrip.pet.repository.PetRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @Mock private PetActivityRepository petActivityRepository;

  private PetService petService;

  @BeforeEach
  void setUp() {
    petService = new PetService(petRepository, breedRepository, petActivityRepository);
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
    Integer breedId = 7;
    when(breedRepository.findById(breedId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3, null))
        .isInstanceOf(BreedNotFoundException.class);
  }

  @Test
  void createPet는_견종이_있으면_반려견을_저장한다() {
    UUID userId = UUID.randomUUID();
    Integer breedId = 7;
    Breed breed = new Breed("골든리트리버");
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(breed));
    when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Pet result = petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3, null);

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

    assertThatThrownBy(() -> petService.updatePet(otherId, petId, null, "루이", null, null, null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void createPet는_활동_ID를_주면_반려견에_연결한다() {
    UUID userId = UUID.randomUUID();
    Integer breedId = 7;
    UUID activityId = UUID.randomUUID();
    PetActivity activity = new PetActivity("산책");
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(new Breed("골든리트리버")));
    when(petActivityRepository.findAllById(Set.of(activityId))).thenReturn(List.of(activity));
    when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Pet result =
        petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3, List.of(activityId));

    assertThat(result.getPreferredActivities()).containsExactly(activity);
  }

  /** 없는 활동 ID를 조용히 무시하면 사용자는 등록됐다고 믿는데 실제로는 빠져 있게 된다. */
  @Test
  void createPet는_없는_활동_ID가_섞이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    Integer breedId = 7;
    UUID realId = UUID.randomUUID();
    UUID ghostId = UUID.randomUUID();
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(new Breed("골든리트리버")));
    when(petActivityRepository.findAllById(Set.of(realId, ghostId)))
        .thenReturn(List.of(new PetActivity("산책")));

    assertThatThrownBy(
            () ->
                petService.createPet(
                    userId, breedId, "초코", PetSize.MEDIUM, 3, List.of(realId, ghostId)))
        .isInstanceOf(PetActivityNotFoundException.class);
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
