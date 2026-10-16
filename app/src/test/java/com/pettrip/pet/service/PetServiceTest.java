package com.pettrip.pet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  @Mock private PhotoRepository photoRepository;
  @Mock private PhotoService photoService;

  private PetService petService;

  @BeforeEach
  void setUp() {
    petService =
        new PetService(
            petRepository, breedRepository, petActivityRepository, photoRepository, photoService);
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

    Pet result = petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3, null).pet();

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

    assertThatThrownBy(
            () -> petService.updatePet(otherId, petId, null, "루이", null, null, null, null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void updatePet는_isDie를_반영한다() {
    UUID ownerId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(ownerId, new Breed("말티즈"), "루이", PetSize.SMALL, 2);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    Pet result = petService.updatePet(ownerId, petId, null, null, null, null, true, null).pet();

    assertThat(result.isDie()).isTrue();
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
        petService.createPet(userId, breedId, "초코", PetSize.MEDIUM, 3, List.of(activityId)).pet();

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

  @Test
  void updateProfilePhoto는_본인_사진인지_확인하고_연결한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    Photo photo = new Photo(userId, null, "profile/u/a.jpg", null);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(pet)).thenReturn(pet);
    when(photoService.getOwnedPhoto(userId, photoId)).thenReturn(photo);
    when(photoRepository.findById(any())).thenReturn(Optional.of(photo));
    when(photoService.issueDownloadUrl("profile/u/a.jpg"))
        .thenReturn(java.net.URI.create("https://bucket/profile/u/a.jpg?sig").toURL());

    PetDetail result = petService.updateProfilePhoto(userId, petId, photoId);

    verify(photoService).getOwnedPhoto(userId, photoId);
    assertThat(result.profilePhoto()).isNotNull();
    assertThat(result.profilePhoto().downloadUrl()).contains("?sig");
  }

  @Test
  void updateProfilePhoto는_photoId가_null이면_연결을_해제한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(pet)).thenReturn(pet);

    PetDetail result = petService.updateProfilePhoto(userId, petId, null);

    assertThat(pet.getProfilePhotoId()).isNull();
    assertThat(result.profilePhoto()).isNull();
    verify(photoService, never()).getOwnedPhoto(any(), any());
  }

  @Test
  void updateProfilePhoto는_남의_반려동물이면_예외를_던진다() {
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(UUID.randomUUID(), new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

    assertThatThrownBy(() -> petService.updateProfilePhoto(UUID.randomUUID(), petId, null))
        .isInstanceOf(PetNotFoundException.class);
  }

  @Test
  void listPets는_사진이_없으면_profilePhoto가_null이다() {
    UUID userId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findByUserId(userId)).thenReturn(List.of(pet));

    List<PetDetail> result = petService.listPets(userId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).profilePhoto()).isNull();
  }

  @Test
  void updateBackgroundPhoto는_본인_사진인지_확인하고_연결한다() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    Photo photo = new Photo(userId, null, "profile/u/bg.jpg", null);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(pet)).thenReturn(pet);
    when(photoService.getOwnedPhoto(userId, photoId)).thenReturn(photo);
    when(photoRepository.findById(any())).thenReturn(Optional.of(photo));
    when(photoService.issueDownloadUrl("profile/u/bg.jpg"))
        .thenReturn(java.net.URI.create("https://bucket/profile/u/bg.jpg?sig").toURL());

    PetDetail result = petService.updateBackgroundPhoto(userId, petId, photoId);

    verify(photoService).getOwnedPhoto(userId, photoId);
    assertThat(result.backgroundPhoto()).isNotNull();
    assertThat(result.backgroundPhoto().downloadUrl()).contains("?sig");
  }

  @Test
  void updateBackgroundPhoto는_photoId가_null이면_연결을_해제한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(pet)).thenReturn(pet);

    PetDetail result = petService.updateBackgroundPhoto(userId, petId, null);

    assertThat(pet.getBackgroundPhotoId()).isNull();
    assertThat(result.backgroundPhoto()).isNull();
    verify(photoService, never()).getOwnedPhoto(any(), any());
  }

  @Test
  void 프로필과_배경화면은_서로_영향을_주지_않는다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
    when(petRepository.save(pet)).thenReturn(pet);

    petService.updateProfilePhoto(userId, petId, null);
    petService.updateBackgroundPhoto(userId, petId, null);

    assertThat(pet.getProfilePhotoId()).isNull();
    assertThat(pet.getBackgroundPhotoId()).isNull();
  }

  @Test
  void listPets는_사진이_없으면_두_뷰가_모두_null이다() {
    UUID userId = UUID.randomUUID();
    Pet pet = new Pet(userId, new Breed("골든리트리버"), "초코", PetSize.MEDIUM, 3);
    when(petRepository.findByUserId(userId)).thenReturn(List.of(pet));

    List<PetDetail> result = petService.listPets(userId);

    assertThat(result.get(0).profilePhoto()).isNull();
    assertThat(result.get(0).backgroundPhoto()).isNull();
  }
}
