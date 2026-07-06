package com.pettrip.user.controller;

import jakarta.validation.constraints.NotBlank;

public record NicknameRegisterRequest(@NotBlank String nickname) {}
