package com.pettrip.photo.controller;

import com.pettrip.common.service.CurrentUserId;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.service.PhotoSaveCommand;
import com.pettrip.photo.service.PhotoService;
import jakarta.validation.Valid;
import java.net.URL;
import java.util.List;
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
  public List<PhotoUploadUrlResponse> issueUploadUrls(
      @CurrentUserId UUID userId, @RequestBody @Valid PhotoUploadUrlRequest request) {
    return request.files().stream().map(file -> toUploadUrl(userId, file)).toList();
  }

  private PhotoUploadUrlResponse toUploadUrl(UUID userId, PhotoUploadUrlRequest.FileRequest file) {
    String photoKey = photoService.buildPhotoKey(userId, file.type(), file.fileName());
    URL uploadUrl = photoService.issueUploadUrl(photoKey);
    return new PhotoUploadUrlResponse(uploadUrl.toString(), photoKey, file.fileName());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public List<PhotoResponse> createPhotos(
      @CurrentUserId UUID userId, @RequestBody @Valid PhotoCreateRequest request) {
    List<PhotoSaveCommand> commands =
        request.photos().stream()
            .map(p -> new PhotoSaveCommand(p.coursePlaceId(), p.photoKey(), p.takenAt()))
            .toList();
    return photoService.savePhotos(userId, commands).stream().map(PhotoResponse::from).toList();
  }

  @GetMapping("/{photoId}")
  public PhotoDownloadResponse getPhoto(@CurrentUserId UUID userId, @PathVariable UUID photoId) {
    Photo photo = photoService.getOwnedPhoto(userId, photoId);
    URL downloadUrl = photoService.issueDownloadUrl(photo.getPhotoUrl());
    return PhotoDownloadResponse.of(photo, downloadUrl.toString());
  }
}
