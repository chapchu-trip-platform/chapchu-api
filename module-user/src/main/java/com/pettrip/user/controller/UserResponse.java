package com.pettrip.user.controller;

import com.pettrip.user.model.AccountStatus;
import com.pettrip.user.model.Role;
import com.pettrip.user.model.User;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String email,
    String nickname,
    Role role,
    AccountStatus accountStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getEmail(),
        user.getNickname(),
        user.getRole(),
        user.getAccountStatus(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
