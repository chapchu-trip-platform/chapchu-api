package com.pettrip.post.controller;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PostCreateRequest(
    @NotNull UUID petId,
    @NotNull UUID photoId,
    @NotNull UUID courseId,
    String title,
    String content) {}
