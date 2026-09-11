package com.pettrip.pet.service;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetActivityRepository;
import com.pettrip.pet.repository.PetRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetService {

  private final PetRepository petRepository;
  private final BreedRepository breedRepository;
  private final PetActivityRepository petActivityRepository;

  public PetService(
      PetRepository petRepository,
      BreedRepository breedRepository,
      PetActivityRepository petActivityRepository) {
    this.petRepository = petRepository;
    this.breedRepository = breedRepository;
    this.petActivityRepository = petActivityRepository;
  }

  @Transactional(readOnly = true)
  public List<Pet> listPets(UUID userId) {
    return petRepository.findByUserId(userId);
  }

  @Transactional
  public Pet createPet(
      UUID userId,
      Integer breedId,
      String petName,
      PetSize size,
      Integer age,
      List<UUID> activityIds) {
    Breed breed = findBreed(breedId);
    Pet pet = new Pet(userId, breed, petName, size, age);
    pet.replaceActivities(findActivities(activityIds));
    return petRepository.save(pet);
  }

  @Transactional
  public Pet updatePet(
      UUID userId,
      UUID petId,
      Integer breedId,
      String petName,
      PetSize size,
      Integer age,
      List<UUID> activityIds) {
    Pet pet = getOwnedPet(userId, petId);
    Breed breed = breedId != null ? findBreed(breedId) : null;
    pet.update(breed, petName, size, age);
    pet.replaceActivities(findActivities(activityIds));
    return petRepository.save(pet);
  }

  @Transactional
  /**
   * 무지개다리를 건넜다고 표시한다. 이 순간부터 그 아이의 앨범은 추억앨범으로 분류된다.
   *
   * <p>되돌리기 어려운 상태 변화라 {@code PATCH /pets/{petId}}(이름·나이 수정)와 분리했다. 실수로 섞여 들어가면 곤란하다.
   */
  public Pet markDie(UUID userId, UUID petId) {
    Pet pet = getOwnedPet(userId, petId);
    pet.markDie();
    return petRepository.save(pet);
  }

  /** 잘못 표시한 경우 되돌린다. */
  public Pet restoreDie(UUID userId, UUID petId) {
    Pet pet = getOwnedPet(userId, petId);
    pet.restoreDie();
    return petRepository.save(pet);
  }

  public void deletePet(UUID userId, UUID petId) {
    Pet pet = getOwnedPet(userId, petId);
    petRepository.delete(pet);
  }

  private Breed findBreed(Integer breedId) {
    return breedRepository.findById(breedId).orElseThrow(BreedNotFoundException::new);
  }

  /**
   * 요청에 담긴 활동 ID를 실제 코드값으로 바꾼다.
   *
   * <p>{@code null}이면 그대로 {@code null}을 돌려준다. "활동을 안 건드림"과 "전부 지움"을 구분해야 하기 때문이다. 하나라도 존재하지 않는 ID가
   * 섞여 있으면 조용히 무시하지 않고 404로 거절한다.
   */
  private Set<PetActivity> findActivities(List<UUID> activityIds) {
    if (activityIds == null) {
      return null;
    }
    Set<UUID> unique = new LinkedHashSet<>(activityIds);
    List<PetActivity> found = petActivityRepository.findAllById(unique);
    if (found.size() != unique.size()) {
      throw new PetActivityNotFoundException();
    }
    return new LinkedHashSet<>(found);
  }

  private Pet getOwnedPet(UUID userId, UUID petId) {
    Pet pet = petRepository.findById(petId).orElseThrow(PetNotFoundException::new);
    if (!pet.getUserId().equals(userId)) {
      throw new PetNotFoundException();
    }
    return pet;
  }
}
