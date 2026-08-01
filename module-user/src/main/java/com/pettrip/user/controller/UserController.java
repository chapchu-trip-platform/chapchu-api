package com.pettrip.user.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.user.model.User;
import com.pettrip.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  public UserResponse getMe() {
    return UserResponse.from(userService.getMe(TempAuthContext.TEMP_USER_ID));
  }

  @PostMapping("/me")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse registerNickname(@RequestBody @Valid NicknameRegisterRequest request) {
    User user = userService.registerNickname(TempAuthContext.TEMP_USER_ID, request.nickname());
    return UserResponse.from(user);
  }

  @PatchMapping("/me")
  public UserResponse updateMe(@RequestBody UserUpdateRequest request) {
    User user =
        userService.updateMe(
            TempAuthContext.TEMP_USER_ID, request.nickname(), request.accountStatus());
    return UserResponse.from(user);
  }

  @GetMapping("/nickname/check")
  public NicknameCheckResponse checkNickname(@RequestParam String nickname) {
    return NicknameCheckResponse.of(userService.isNicknameAvailable(nickname));
  }
}
