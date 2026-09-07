package com.pettrip.review.controller;

import com.pettrip.review.service.AlbumItem;
import java.time.LocalDate;
import java.util.UUID;

public record AlbumPhotoResponse(
    UUID photoId, String downloadUrl, LocalDate takenAt, UUID reviewId, String placeId) {

  public static AlbumPhotoResponse of(AlbumItem item) {
    return new AlbumPhotoResponse(
        item.photoId(), item.downloadUrl(), item.takenAt(), item.reviewId(), item.placeId());
  }
}
