package com.pettrip.signup.controller;

import com.pettrip.pet.model.Pet;
import com.pettrip.user.model.User;
import java.util.List;
import java.util.UUID;

/**
 * @param userId 만들어진 유저 ID. 발급될 access token의 {@code sub}와 같다
 * @param petIds 만들어진 반려동물 ID. 요청 순서를 그대로 지킨다
 */
public record SignupResponse(UUID userId, String nickname, String email, List<UUID> petIds) {

  public static SignupResponse of(User user, List<Pet> pets) {
    return new SignupResponse(
        user.getId(), user.getNickname(), user.getEmail(), pets.stream().map(Pet::getId).toList());
  }
}
