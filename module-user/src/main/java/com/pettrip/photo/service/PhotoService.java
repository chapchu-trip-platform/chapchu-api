package com.pettrip.photo.service;

import com.pettrip.photo.model.Photo;
import com.pettrip.photo.model.PhotoType;
import com.pettrip.photo.repository.PhotoRepository;
import io.awspring.cloud.s3.S3Operations;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PhotoService {

  private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(10);
  private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(10);

  private final PhotoRepository photoRepository;
  private final S3Operations s3Operations;
  private final String bucket;

  public PhotoService(
      PhotoRepository photoRepository,
      S3Operations s3Operations,
      @Value("${app.s3.bucket}") String bucket) {
    this.photoRepository = photoRepository;
    this.s3Operations = s3Operations;
    this.bucket = bucket;
  }

  public URL issueUploadUrl(String photoKey) {
    return s3Operations.createSignedPutURL(bucket, photoKey, UPLOAD_URL_DURATION);
  }

  public String buildPhotoKey(UUID userId, PhotoType type, String fileName) {
    return "%s/%s/%s-%s".formatted(type.folder(), userId, UUID.randomUUID(), fileName);
  }

  public Photo savePhoto(UUID userId, UUID coursePlaceId, String photoKey, LocalDate takenAt) {
    return photoRepository.save(new Photo(userId, coursePlaceId, photoKey, takenAt));
  }

  public Photo getOwnedPhoto(UUID userId, UUID photoId) {
    Photo photo = photoRepository.findById(photoId).orElseThrow(PhotoNotFoundException::new);
    if (!userId.equals(photo.getUserId())) {
      throw new PhotoNotFoundException();
    }
    return photo;
  }

  public URL issueDownloadUrl(String photoKey) {
    return s3Operations.createSignedGetURL(bucket, photoKey, DOWNLOAD_URL_DURATION);
  }
}
