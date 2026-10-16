package com.pettrip.user.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.service.UserService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/preferences")
public class UserPreferenceController {

  private final UserService userService;

  public UserPreferenceController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public PreferenceResponse getPreferences(@CurrentUserId UUID userId) {
    return PreferenceResponse.from(userService.getPreferences(userId));
  }

  /**
   * 선호 사항을 수정한다.
   *
   * <p>최초 등록과 이후 수정을 나누지 않는다. 선호 사항은 유저에 종속된 단일 값이라 따로 생성되는 리소스가 아니고, 서버가 두 경우를 구분할 이유가 없다. 최초 등록은
   * {@code POST /auth/signup}이 회원가입과 함께 처리한다.
   *
   * <p>{@code PATCH}인 이유는 {@code null}로 온 항목을 건드리지 않기 때문이다. 생략한 항목을 비우는 {@code PUT} 의미가 아니다.
   */
  @PatchMapping
  public PreferenceResponse updatePreferences(
      @CurrentUserId UUID userId, @RequestBody PreferenceRequest request) {
    return PreferenceResponse.from(
        userService.updatePreferences(
            userId, request.regionIds(), request.themeIds(), request.transportMethodIds()));
  }
}
