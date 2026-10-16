package com.pettrip.comment.controller;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateRequest(@NotBlank String content) {}
