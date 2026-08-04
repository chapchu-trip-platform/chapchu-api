package com.pettrip.user.controller;

import com.pettrip.user.service.PreferenceOptionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 선호 사항 선택지 조회.
 *
 * <p>유저별 데이터가 아니라 공용 코드값이므로 {@code /users/me} 아래가 아니라 별도 경로에 둔다.
 */
@RestController
@RequestMapping("/preferences/options")
public class PreferenceOptionController {

  private final PreferenceOptionService preferenceOptionService;

  public PreferenceOptionController(PreferenceOptionService preferenceOptionService) {
    this.preferenceOptionService = preferenceOptionService;
  }

  @GetMapping
  public PreferenceOptionsResponse getOptions() {
    return PreferenceOptionsResponse.of(
        preferenceOptionService.findRegions(),
        preferenceOptionService.findThemes(),
        preferenceOptionService.findTransportMethods());
  }
}
