package com.pettrip.photo.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PhotoCreateRequest(@NotEmpty @Valid List<PhotoEntry> photos) {

  public record PhotoEntry(UUID coursePlaceId, @NotBlank String photoKey, LocalDate takenAt) {}
}
