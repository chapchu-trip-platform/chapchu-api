package com.pettrip.user.controller;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Role;
import com.pettrip.user.model.User;
import com.pettrip.user.service.MeDetail;
import com.pettrip.user.service.ProfilePhotoView;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String nickname,
    Role role,
    AccountStatus accountStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    ProfilePhotoView profilePhoto) {

  public static UserResponse of(MeDetail detail) {
    User user = detail.user();
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getRole(),
        user.getAccountStatus(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        detail.profilePhoto());
  }
}
