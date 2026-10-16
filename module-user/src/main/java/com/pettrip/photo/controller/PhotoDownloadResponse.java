package com.pettrip.photo.controller;

import com.pettrip.photo.model.Photo;
import java.time.LocalDate;
import java.util.UUID;

public record PhotoDownloadResponse(UUID id, String downloadUrl, LocalDate takenAt) {

  public static PhotoDownloadResponse of(Photo photo, String downloadUrl) {
    return new PhotoDownloadResponse(photo.getId(), downloadUrl, photo.getTakenAt());
  }
}
