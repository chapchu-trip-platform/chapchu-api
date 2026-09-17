package com.pettrip.pet.service;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.model.PetActivity;
import com.pettrip.pet.model.PetSize;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetActivityRepository;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.repository.PhotoRepository;
import com.pettrip.photo.service.PhotoService;
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
  private final PhotoRepository photoRepository;
  private final PhotoService photoService;

  public PetService(
      PetRepository petRepository,
      BreedRepository breedRepository,
      PetActivityRepository petActivityRepository,
      PhotoRepository photoRepository,
      PhotoService photoService) {
    this.petRepository = petRepository;
    this.breedRepository = breedRepository;
    this.petActivityRepository = petActivityRepository;
    this.photoRepository = photoRepository;
    this.photoService = photoService;
  }

  @Transactional(readOnly = true)
  public List<PetDetail> listPets(UUID userId) {
    return petRepository.findByUserId(userId).stream().map(this::assemble).toList();
  }

  /**
   * 프로필 사진을 연결하거나 해제한다.
   *
   * <p>{@code photoId}가 null이면 해제한다. 값이 있으면 본인 사진인지 먼저 확인한다 — 남의 사진 id를 붙이지 못하게 막는다.
   *
   * <p>사진 교체는 별도 UI 동작이라 {@code PATCH /pets/{petId}}(이름·나이 수정)와 분리했다. 유저 프사도 {@code PATCH
   * /users/me/photo}로 나뉘어 있다.
   */
  @Transactional
  public PetDetail updateProfilePhoto(UUID userId, UUID petId, UUID photoId) {
    Pet pet = getOwnedPet(userId, petId);
    verifyPhotoOwnership(userId, photoId);
    pet.updateProfilePhoto(photoId);
    return assemble(petRepository.save(pet));
  }

  /** 프로필 배경화면을 연결하거나 해제한다. 사진은 프로필과 같은 경로(type=PROFILE)에 올린다. */
  @Transactional
  public PetDetail updateBackgroundPhoto(UUID userId, UUID petId, UUID photoId) {
    Pet pet = getOwnedPet(userId, petId);
    verifyPhotoOwnership(userId, photoId);
    pet.updateBackgroundPhoto(photoId);
    return assemble(petRepository.save(pet));
  }

  private void verifyPhotoOwnership(UUID userId, UUID photoId) {
    if (photoId == null) {
      return;
    }
    photoService.getOwnedPhoto(userId, photoId);
  }

  /** 사진이 없으면 해당 뷰를 null로 둔다. 기본 이미지는 프론트가 처리한다. */
  private PetDetail assemble(Pet pet) {
    return new PetDetail(
        pet, photoView(pet.getProfilePhotoId()), photoView(pet.getBackgroundPhotoId()));
  }

  private PetPhotoView photoView(UUID photoId) {
    if (photoId == null) {
      return null;
    }
    Photo photo = photoRepository.findById(photoId).orElse(null);
    if (photo == null) {
      return null;
    }
    return new PetPhotoView(
        photo.getId(), photoService.issueDownloadUrl(photo.getPhotoUrl()).toString());
  }

  @Transactional
  public PetDetail createPet(
      UUID userId,
      Integer breedId,
      String petName,
      PetSize size,
      Integer age,
      List<UUID> activityIds) {
    Breed breed = findBreed(breedId);
    Pet pet = new Pet(userId, breed, petName, size, age);
    pet.replaceActivities(findActivities(activityIds));
    return assemble(petRepository.save(pet));
  }

  @Transactional
  public PetDetail updatePet(
      UUID userId,
      UUID petId,
      Integer breedId,
      String petName,
      PetSize size,
      Integer age,
      Boolean isDie,
      List<UUID> activityIds) {
    Pet pet = getOwnedPet(userId, petId);
    Breed breed = breedId != null ? findBreed(breedId) : null;
    pet.update(breed, petName, size, age);
    pet.updateIsDie(isDie);
    pet.replaceActivities(findActivities(activityIds));
    return assemble(petRepository.save(pet));
  }

  @Transactional
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
