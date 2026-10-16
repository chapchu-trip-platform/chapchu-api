package com.pettrip.photo.controller;

import com.pettrip.photo.model.PhotoType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PhotoUploadUrlRequest(@NotEmpty @Valid List<FileRequest> files) {

  public record FileRequest(@NotNull PhotoType type, @NotBlank String fileName) {}
}
