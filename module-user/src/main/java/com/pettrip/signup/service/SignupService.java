package com.pettrip.signup.service;

import com.pettrip.pet.model.Breed;
import com.pettrip.pet.model.Pet;
import com.pettrip.pet.repository.BreedRepository;
import com.pettrip.pet.repository.PetActivityRepository;
import com.pettrip.pet.repository.PetRepository;
import com.pettrip.pet.service.BreedNotFoundException;
import com.pettrip.pet.service.PetActivityNotFoundException;
import com.pettrip.signup.controller.SignupRequest;
import com.pettrip.signup.service.RegistrationTokenClient.VerifiedRegistration;
import com.pettrip.user.model.User;
import com.pettrip.user.repository.RegionRepository;
import com.pettrip.user.repository.ThemeRepository;
import com.pettrip.user.repository.TransportMethodRepository;
import com.pettrip.user.repository.UserRepository;
import com.pettrip.user.service.NicknameAlreadyInUseException;
import com.pettrip.user.service.RegionNotFoundException;
import com.pettrip.user.service.ThemeNotFoundException;
import com.pettrip.user.service.TransportMethodNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 온보딩에서 유저·선호 사항·반려동물을 한 번에 만든다.
 *
 * <p>예전에는 반려동물 두 마리를 등록하려면 다섯 번을 호출해야 했다.
 *
 * <pre>
 * POST /auth/register → (구글 재로그인) → POST /users/me/preferences → POST /pets → POST /pets
 * </pre>
 *
 * <p>중간에 하나라도 실패하면 절반만 만들어진 계정이 남았다. 여기서는 전부 한 트랜잭션이라 도중에 실패하면 아무것도 남지 않고, 사용자는 같은 토큰으로 그대로 재시도할 수
 * 있다.
 *
 * <p>토큰 검증만 chapchu-auth에 맡기지만 그건 <b>읽기 전용</b>이라 이 트랜잭션이 뒤집혀도 저쪽에 흔적이 남지 않는다.
 */
@Service
public class SignupService {

  private final RegistrationTokenClient registrationTokenClient;
  private final UserRepository userRepository;
  private final RegionRepository regionRepository;
  private final ThemeRepository themeRepository;
  private final TransportMethodRepository transportMethodRepository;
  private final PetRepository petRepository;
  private final BreedRepository breedRepository;
  private final PetActivityRepository petActivityRepository;

  public SignupService(
      RegistrationTokenClient registrationTokenClient,
      UserRepository userRepository,
      RegionRepository regionRepository,
      ThemeRepository themeRepository,
      TransportMethodRepository transportMethodRepository,
      PetRepository petRepository,
      BreedRepository breedRepository,
      PetActivityRepository petActivityRepository) {
    this.registrationTokenClient = registrationTokenClient;
    this.userRepository = userRepository;
    this.regionRepository = regionRepository;
    this.themeRepository = themeRepository;
    this.transportMethodRepository = transportMethodRepository;
    this.petRepository = petRepository;
    this.breedRepository = breedRepository;
    this.petActivityRepository = petActivityRepository;
  }

  @Transactional
  public SignupResult signUp(SignupRequest request) {
    VerifiedRegistration verified = registrationTokenClient.verify(request.registrationToken());

    if (userRepository.existsByGoogleUserId(verified.googleUserId())) {
      throw new AlreadySignedUpException();
    }
    if (userRepository.existsByNickname(request.user().nickname())) {
      throw new NicknameAlreadyInUseException();
    }

    User user = createUser(verified, request.user());
    List<Pet> pets = createPets(user.getId(), request.pets());

    return new SignupResult(user, pets);
  }

  private User createUser(VerifiedRegistration verified, SignupRequest.UserPart part) {
    User user = new User(verified.email(), verified.googleUserId());
    user.registerNickname(part.nickname());

    if (part.regionIds() != null) {
      user.replacePreferredRegions(
          new HashSet<>(
              resolve(
                  part.regionIds(), regionRepository::findAllById, RegionNotFoundException::new)));
    }
    if (part.themeIds() != null) {
      user.replacePreferredThemes(
          new HashSet<>(
              resolve(part.themeIds(), themeRepository::findAllById, ThemeNotFoundException::new)));
    }
    if (part.transportMethodIds() != null) {
      user.replacePreferredTransportMethods(
          new HashSet<>(
              resolve(
                  part.transportMethodIds(),
                  transportMethodRepository::findAllById,
                  TransportMethodNotFoundException::new)));
    }

    // saveAndFlush 여야 UNIQUE 위반이 이 안에서 터진다. 위 existsBy 검사와 실제 저장 사이를 다른 요청이
    // 비집고 들어올 수 있어 DB 제약이 마지막 방어선이다.
    return userRepository.saveAndFlush(user);
  }

  private List<Pet> createPets(UUID userId, List<SignupRequest.PetPart> parts) {
    if (parts == null || parts.isEmpty()) {
      return List.of();
    }
    List<Pet> saved = new ArrayList<>();
    for (SignupRequest.PetPart part : parts) {
      Breed breed =
          breedRepository.findById(part.breedId()).orElseThrow(BreedNotFoundException::new);
      Pet pet = new Pet(userId, breed, part.petName(), part.size(), part.age());
      if (part.activityIds() != null) {
        pet.replaceActivities(
            new LinkedHashSet<>(
                resolve(
                    part.activityIds(),
                    petActivityRepository::findAllById,
                    PetActivityNotFoundException::new)));
      }
      saved.add(petRepository.save(pet));
    }
    return saved;
  }

  /**
   * ID 목록을 실제 코드값으로 바꾼다. 하나라도 없으면 조용히 빼지 않고 거절한다.
   *
   * <p>조용히 무시하면 사용자는 고른 대로 저장됐다고 믿는데 실제로는 빠져 있게 된다.
   */
  private <T> List<T> resolve(
      List<UUID> ids,
      Function<Set<UUID>, List<T>> finder,
      Supplier<? extends RuntimeException> onMissing) {
    Set<UUID> unique = new LinkedHashSet<>(ids);
    List<T> found = finder.apply(unique);
    if (found.size() != unique.size()) {
      throw onMissing.get();
    }
    return found;
  }

  public record SignupResult(User user, List<Pet> pets) {}
}
