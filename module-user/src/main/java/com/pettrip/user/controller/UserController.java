package com.pettrip.user.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.model.User;
import com.pettrip.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public UserResponse getMe(@CurrentUserId UUID userId) {
    return UserResponse.from(userService.getMe(userId));
  }

  @PatchMapping("/nickname")
  public UserResponse changeNickname(
      @CurrentUserId UUID userId, @RequestBody @Valid NicknameChangeRequest request) {
    User user = userService.updateMe(userId, request.nickname(), null);
    return UserResponse.from(user);
  }

  @PatchMapping
  public UserResponse updateMe(@CurrentUserId UUID userId, @RequestBody UserUpdateRequest request) {
    User user = userService.updateMe(userId, request.nickname(), request.accountStatus());
    return UserResponse.from(user);
  }
}
