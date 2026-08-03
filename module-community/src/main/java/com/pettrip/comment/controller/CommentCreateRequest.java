package com.pettrip.comment.controller;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CommentCreateRequest(UUID parentCommentId, @NotBlank String content) {}
