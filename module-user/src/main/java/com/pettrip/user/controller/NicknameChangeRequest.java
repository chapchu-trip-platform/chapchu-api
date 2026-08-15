package com.pettrip.user.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NicknameChangeRequest(@NotBlank @Size(max = 30) String nickname) {}
