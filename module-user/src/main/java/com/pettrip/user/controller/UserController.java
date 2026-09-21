package com.pettrip.user.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.user.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    return UserResponse.of(userService.getMe(userId));
  }

  @PatchMapping("/nickname")
  public UserResponse changeNickname(
      @CurrentUserId UUID userId, @RequestBody @Valid NicknameChangeRequest request) {
    return UserResponse.of(userService.updateMe(userId, request.nickname(), null));
  }

  @PatchMapping
  public UserResponse updateMe(@CurrentUserId UUID userId, @RequestBody UserUpdateRequest request) {
    return UserResponse.of(
        userService.updateMe(userId, request.nickname(), request.accountStatus()));
  }

  @PatchMapping("/photo")
  public UserResponse updateProfilePhoto(
      @CurrentUserId UUID userId, @RequestBody ProfilePhotoUpdateRequest request) {
    return UserResponse.of(userService.updateProfilePhoto(userId, request.photoId()));
  }

  /** 회원 탈퇴. JWT 주인 본인 계정을 WITHDRAWN으로 바꾼다(소프트 탈퇴). */
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void withdraw(@CurrentUserId UUID userId) {
    userService.withdraw(userId);
  }
}
