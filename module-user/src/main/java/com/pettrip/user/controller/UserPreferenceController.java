package com.pettrip.user.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.service.UserService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PreferenceResponse registerPreferences(
      @CurrentUserId UUID userId, @RequestBody PreferenceRequest request) {
    return PreferenceResponse.from(
        userService.updatePreferences(
            userId, request.regionIds(), request.themeIds(), request.transportMethodIds()));
  }

  @PatchMapping
  public PreferenceResponse updatePreferences(
      @CurrentUserId UUID userId, @RequestBody PreferenceRequest request) {
    return PreferenceResponse.from(
        userService.updatePreferences(
            userId, request.regionIds(), request.themeIds(), request.transportMethodIds()));
  }
}
