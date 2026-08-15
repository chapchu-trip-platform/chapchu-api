package com.pettrip.signup.service;

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
import com.pettrip.pet.service.BreedNotFoundException;
import com.pettrip.pet.service.PetActivityNotFoundException;
import com.pettrip.signup.controller.SignupRequest;
import com.pettrip.signup.service.RegistrationTokenClient.VerifiedRegistration;
import com.pettrip.signup.service.SignupService.SignupResult;
import com.pettrip.user.model.Region;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.NicknameAlreadyInUseException;
import com.pettrip.user.service.RegionNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignupServiceTest {

  @Mock private RegistrationTokenClient registrationTokenClient;
  @Mock private UserRepository userRepository;
  @Mock private RegionRepository regionRepository;
  @Mock private ThemeRepository themeRepository;
  @Mock private TransportMethodRepository transportMethodRepository;
  @Mock private PetRepository petRepository;
  @Mock private BreedRepository breedRepository;
  @Mock private PetActivityRepository petActivityRepository;

  private SignupService signupService;

  @BeforeEach
  void setUp() {
    signupService =
        new SignupService(
            registrationTokenClient,
            userRepository,
            regionRepository,
            themeRepository,
            transportMethodRepository,
            petRepository,
            breedRepository,
            petActivityRepository);

    when(registrationTokenClient.verify(any()))
        .thenReturn(new VerifiedRegistration("google-001", "user@example.com"));
    when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));
    when(petRepository.save(any(Pet.class))).thenAnswer(i -> i.getArgument(0));
  }

  private SignupRequest request(SignupRequest.UserPart user, List<SignupRequest.PetPart> pets) {
    return new SignupRequest("토큰.서명", user, pets);
  }

  private SignupRequest.UserPart plainUser() {
    return new SignupRequest.UserPart("밤톨이아빠", null, null, null);
  }

  @Test
  void 유저와_반려동물을_한_번에_만든다() {
    Integer breedId = 7;
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(new Breed("푸들")));

    SignupResult result =
        signupService.signUp(
            request(
                plainUser(),
                List.of(
                    new SignupRequest.PetPart("초코", breedId, PetSize.SMALL, 3, null),
                    new SignupRequest.PetPart("보리", breedId, PetSize.MEDIUM, 5, null))));

    assertThat(result.user().getNickname()).isEqualTo("밤톨이아빠");
    assertThat(result.user().getEmail()).isEqualTo("user@example.com");
    assertThat(result.user().getGoogleUserId()).isEqualTo("google-001");
    assertThat(result.pets()).extracting(Pet::getPetName).containsExactly("초코", "보리");
  }

  @Test
  @DisplayName("반려동물이 없어도 가입된다")
  void signsUpWithoutPets() {
    SignupResult result = signupService.signUp(request(plainUser(), null));

    assertThat(result.pets()).isEmpty();
    verify(petRepository, never()).save(any());
  }

  @Test
  void 선호_사항을_함께_저장한다() {
    UUID regionId = UUID.randomUUID();
    Region seoul = new Region("서울");
    when(regionRepository.findAllById(Set.of(regionId))).thenReturn(List.of(seoul));

    SignupResult result =
        signupService.signUp(
            request(new SignupRequest.UserPart("밤톨이아빠", List.of(regionId), null, null), null));

    assertThat(result.user().getPreferredRegions()).containsExactly(seoul);
  }

  @Test
  void 반려동물의_선호_활동을_연결한다() {
    Integer breedId = 7;
    UUID activityId = UUID.randomUUID();
    PetActivity walk = new PetActivity("산책");
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(new Breed("푸들")));
    when(petActivityRepository.findAllById(Set.of(activityId))).thenReturn(List.of(walk));

    SignupResult result =
        signupService.signUp(
            request(
                plainUser(),
                List.of(
                    new SignupRequest.PetPart(
                        "초코", breedId, PetSize.SMALL, 3, List.of(activityId)))));

    assertThat(result.pets().get(0).getPreferredActivities()).containsExactly(walk);
  }

  @Test
  @DisplayName("이미 가입한 구글 계정이면 거절한다")
  void rejectsAlreadySignedUp() {
    when(userRepository.existsByGoogleUserId("google-001")).thenReturn(true);

    assertThatThrownBy(() -> signupService.signUp(request(plainUser(), null)))
        .isInstanceOf(AlreadySignedUpException.class);

    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("닉네임이 이미 쓰이고 있으면 거절한다")
  void rejectsTakenNickname() {
    when(userRepository.existsByNickname("밤톨이아빠")).thenReturn(true);

    assertThatThrownBy(() -> signupService.signUp(request(plainUser(), null)))
        .isInstanceOf(NicknameAlreadyInUseException.class);
  }

  /** 없는 코드값을 조용히 빼면 사용자는 고른 대로 저장됐다고 믿는데 실제로는 빠져 있게 된다. */
  @Test
  @DisplayName("없는 지역 ID가 섞이면 거절한다")
  void rejectsUnknownRegion() {
    UUID real = UUID.randomUUID();
    UUID ghost = UUID.randomUUID();
    when(regionRepository.findAllById(Set.of(real, ghost))).thenReturn(List.of(new Region("서울")));

    assertThatThrownBy(
            () ->
                signupService.signUp(
                    request(
                        new SignupRequest.UserPart("밤톨이아빠", List.of(real, ghost), null, null),
                        null)))
        .isInstanceOf(RegionNotFoundException.class);
  }

  @Test
  @DisplayName("없는 견종이면 거절한다")
  void rejectsUnknownBreed() {
    Integer breedId = 7;
    when(breedRepository.findById(breedId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                signupService.signUp(
                    request(
                        plainUser(),
                        List.of(new SignupRequest.PetPart("초코", breedId, PetSize.SMALL, 3, null)))))
        .isInstanceOf(BreedNotFoundException.class);
  }

  @Test
  @DisplayName("없는 활동 ID가 섞이면 거절한다")
  void rejectsUnknownActivity() {
    Integer breedId = 7;
    UUID real = UUID.randomUUID();
    UUID ghost = UUID.randomUUID();
    when(breedRepository.findById(breedId)).thenReturn(Optional.of(new Breed("푸들")));
    when(petActivityRepository.findAllById(Set.of(real, ghost)))
        .thenReturn(List.of(new PetActivity("산책")));

    assertThatThrownBy(
            () ->
                signupService.signUp(
                    request(
                        plainUser(),
                        List.of(
                            new SignupRequest.PetPart(
                                "초코", breedId, PetSize.SMALL, 3, List.of(real, ghost))))))
        .isInstanceOf(PetActivityNotFoundException.class);
  }

  /** 토큰이 유효하지 않으면 아무것도 만들지 않고 즉시 멈춰야 한다. */
  @Test
  @DisplayName("토큰 검증에 실패하면 아무것도 만들지 않는다")
  void createsNothingWhenTokenInvalid() {
    when(registrationTokenClient.verify(any())).thenThrow(new InvalidRegistrationTokenException());

    assertThatThrownBy(() -> signupService.signUp(request(plainUser(), null)))
        .isInstanceOf(InvalidRegistrationTokenException.class);

    verify(userRepository, never()).saveAndFlush(any());
    verify(petRepository, never()).save(any());
  }
}
