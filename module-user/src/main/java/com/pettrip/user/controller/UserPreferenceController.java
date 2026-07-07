package com.pettrip.user.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.user.service.UserService;
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
  public PreferenceResponse getPreferences() {
    return PreferenceResponse.from(userService.getPreferences(TempAuthContext.TEMP_USER_ID));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PreferenceResponse registerPreferences(@RequestBody PreferenceRequest request) {
    return PreferenceResponse.from(
        userService.updatePreferences(
            TempAuthContext.TEMP_USER_ID,
            request.regionIds(),
            request.themeIds(),
            request.transportMethodIds()));
  }

  @PatchMapping
  public PreferenceResponse updatePreferences(@RequestBody PreferenceRequest request) {
    return PreferenceResponse.from(
        userService.updatePreferences(
            TempAuthContext.TEMP_USER_ID,
            request.regionIds(),
            request.themeIds(),
            request.transportMethodIds()));
  }
}
