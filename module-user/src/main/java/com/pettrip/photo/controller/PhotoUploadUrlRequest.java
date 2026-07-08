package com.pettrip.photo.controller;

import jakarta.validation.constraints.NotBlank;

public record PhotoUploadUrlRequest(@NotBlank String fileName) {}
