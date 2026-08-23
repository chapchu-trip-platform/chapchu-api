package com.pettrip.signup.controller;

import com.pettrip.pet.model.PetSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 온보딩 한 번에 받는 회원가입 요청.
 *
 * <p>예전에는 반려동물 두 마리를 등록하려면 다섯 번을 호출해야 했다.
 *
 * <pre>
 * POST /auth/register → (구글 재로그인) → POST /users/me/preferences → POST /pets → POST /pets
 * </pre>
 *
 * @param registrationToken 온보딩 리다이렉트에서 받은 {@code registration_token}. 10분 TTL
 * @param pets 생략하거나 비워도 된다. 나중에 {@code POST /pets}로 추가할 수 있다
 */
public record SignupRequest(
    @NotBlank String registrationToken, @NotNull @Valid UserPart user, @Valid List<PetPart> pets) {

  /**
   * @param regionIds 선호 지역. {@code GET /preferences/options}로 선택지를 받는다
   * @param transportMethodIds 이동수단은 여러 개를 고를 수 있어 지역·테마와 같이 배열로 받는다
   * @param locationConsent 위치 정보 수집 동의 여부. 필수다. 생략하면 400
   */
  public record UserPart(
      @NotBlank @Size(max = 30) String nickname,
      List<UUID> regionIds,
      List<UUID> themeIds,
      List<UUID> transportMethodIds,
      @NotNull Boolean locationConsent) {}

  /**
   * @param breedId {@code GET /breeds}로 선택지를 받는 정수 ID. 순종이 아니면 `믹스견`을 고른다
   * @param activityIds {@code GET /activities}로 선택지를 받는다. 생략 가능
   */
  public record PetPart(
      @NotBlank String petName,
      @NotNull Integer breedId,
      @NotNull PetSize size,
      @NotNull @Min(0) Integer age,
      List<UUID> activityIds) {}
}
