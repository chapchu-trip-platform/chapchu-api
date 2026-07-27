package com.pettrip.photo.controller;

import com.pettrip.common.service.TempAuthContext;
import com.pettrip.photo.model.Photo;
import com.pettrip.photo.service.PhotoService;
import jakarta.validation.Valid;
import java.net.URL;
import org.springframework.http.HttpStatus;
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
  public PhotoUploadUrlResponse issueUploadUrl(@RequestBody @Valid PhotoUploadUrlRequest request) {
    String photoKey = photoService.buildPhotoKey(TempAuthContext.TEMP_USER_ID, request.fileName());
    URL uploadUrl = photoService.issueUploadUrl(photoKey);
    return new PhotoUploadUrlResponse(uploadUrl.toString(), photoKey);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PhotoResponse createPhoto(@RequestBody @Valid PhotoCreateRequest request) {
    Photo photo =
        photoService.savePhoto(
            TempAuthContext.TEMP_USER_ID,
            request.coursePlaceId(),
            request.photoKey(),
            request.takenAt());
    return PhotoResponse.from(photo);
  }
}
