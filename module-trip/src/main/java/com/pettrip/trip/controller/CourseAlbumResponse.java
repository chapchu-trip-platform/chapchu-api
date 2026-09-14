package com.pettrip.trip.controller;

import com.pettrip.trip.service.AlbumService.AlbumPhoto;
import com.pettrip.trip.service.AlbumService.CourseAlbum;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** 코스 단위 앨범 응답. photos의 downloadUrl은 presigned GET(10분), isPublic은 리뷰 등록(공개) 여부. */
public record CourseAlbumResponse(
    UUID courseId, LocalDate travelDate, UUID petId, List<PhotoItem> photos) {

  public record PhotoItem(
      UUID photoId,
      String downloadUrl,
      LocalDate takenAt,
      String externalPlaceId,
      boolean isPublic) {}

  public static CourseAlbumResponse from(CourseAlbum album) {
    return new CourseAlbumResponse(
        album.courseId(),
        album.travelDate(),
        album.petId(),
        album.photos().stream().map(CourseAlbumResponse::toItem).toList());
  }

  private static PhotoItem toItem(AlbumPhoto p) {
    return new PhotoItem(
        p.photoId(), p.downloadUrl(), p.takenAt(), p.externalPlaceId(), p.isPublic());
  }
}
