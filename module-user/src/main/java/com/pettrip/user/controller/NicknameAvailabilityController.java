package com.pettrip.user.controller;

import com.pettrip.user.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * docs/decisions/027 참고: 닉네임 중복 확인.
 *
 * <p>결과는 조회 시점 기준이며 예약 효과가 없다. 확인 후 등록까지의 사이에 다른 유저가 같은 닉네임을 선점할 수 있으므로, 최종 판정은 등록/수정 요청의 409 응답이다.
 */
@RestController
@RequestMapping("/users/nickname/availability")
@Validated
public class NicknameAvailabilityController {

  private final UserService userService;

  public NicknameAvailabilityController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public NicknameAvailabilityResponse checkAvailability(
      @RequestParam @NotBlank @Size(max = 30) String nickname) {
    return new NicknameAvailabilityResponse(nickname, userService.isNicknameAvailable(nickname));
  }
}
