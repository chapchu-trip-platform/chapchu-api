package com.pettrip.photo.controller;

import com.pettrip.photo.model.Photo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PhotoResponse(
    UUID id, UUID coursePlaceId, String photoKey, LocalDate takenAt, LocalDateTime createdAt) {

  public static PhotoResponse from(Photo photo) {
    return new PhotoResponse(
        photo.getId(),
        photo.getCoursePlaceId(),
        photo.getPhotoUrl(),
        photo.getTakenAt(),
        photo.getCreatedAt());
  }
}
