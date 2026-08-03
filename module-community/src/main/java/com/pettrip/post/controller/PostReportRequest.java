package com.pettrip.post.controller;

import jakarta.validation.constraints.NotBlank;

public record PostReportRequest(@NotBlank String reportReason, String reportDetail) {}
