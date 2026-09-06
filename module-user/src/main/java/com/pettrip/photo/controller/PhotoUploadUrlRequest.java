package com.pettrip.photo.controller;

import com.pettrip.photo.model.PhotoType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PhotoUploadUrlRequest(@NotNull PhotoType type, @NotBlank String fileName) {}
