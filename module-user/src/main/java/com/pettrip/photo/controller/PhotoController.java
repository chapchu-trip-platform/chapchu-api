package com.pettrip.photo.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.service.PhotoService;
import jakarta.validation.Valid;
import java.net.URL;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/photos")
public class PhotoController {

  private final PhotoService photoService;

  public PhotoController(PhotoService photoService) {
    this.photoService = photoService;
  }

  @PostMapping("/upload-url")
  @ResponseStatus(HttpStatus.CREATED)
  public PhotoUploadUrlResponse issueUploadUrl(
      @CurrentUserId UUID userId, @RequestBody @Valid PhotoUploadUrlRequest request) {
    String photoKey = photoService.buildPhotoKey(userId, request.type(), request.fileName());
    URL uploadUrl = photoService.issueUploadUrl(photoKey);
    return new PhotoUploadUrlResponse(uploadUrl.toString(), photoKey);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PhotoResponse createPhoto(
      @CurrentUserId UUID userId, @RequestBody @Valid PhotoCreateRequest request) {
    Photo photo =
        photoService.savePhoto(
            userId, request.coursePlaceId(), request.photoKey(), request.takenAt());
    return PhotoResponse.from(photo);
  }

  @GetMapping("/{photoId}")
  public PhotoDownloadResponse getPhoto(@CurrentUserId UUID userId, @PathVariable UUID photoId) {
    Photo photo = photoService.getOwnedPhoto(userId, photoId);
    URL downloadUrl = photoService.issueDownloadUrl(photo.getPhotoUrl());
    return PhotoDownloadResponse.of(photo, downloadUrl.toString());
  }
}
