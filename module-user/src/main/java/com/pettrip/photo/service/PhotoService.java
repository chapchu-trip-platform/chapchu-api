package com.pettrip.photo.service;

import com.pettrip.photo.model.Photo;
import com.pettrip.photo.model.PhotoType;
import com.pettrip.photo.repository.PhotoRepository;
import io.awspring.cloud.s3.S3Operations;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhotoService {

  private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(10);
  private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(10);
  private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

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

  public URL issueUploadUrl(String photoKey, String contentType) {
    return s3Operations.createSignedPutURL(
        bucket, photoKey, UPLOAD_URL_DURATION, null, contentType);
  }

  /** 파일명 확장자로 업로드 Content-Type을 추론한다. FE는 PUT 시 이 값과 동일한 헤더를 보내야 한다. */
  public String contentTypeFor(String fileName) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lower.endsWith(".png")) {
      return "image/png";
    }
    if (lower.endsWith(".webp")) {
      return "image/webp";
    }
    if (lower.endsWith(".gif")) {
      return "image/gif";
    }
    if (lower.endsWith(".heic")) {
      return "image/heic";
    }
    if (lower.endsWith(".svg")) {
      return "image/svg+xml";
    }
    return DEFAULT_CONTENT_TYPE;
  }

  public String buildPhotoKey(UUID userId, PhotoType type, String fileName) {
    return "%s/%s/%s-%s".formatted(type.folder(), userId, UUID.randomUUID(), fileName);
  }

  @Transactional
  public List<Photo> savePhotos(UUID userId, List<PhotoSaveCommand> commands) {
    return commands.stream().map(c -> saveOne(userId, c)).toList();
  }

  private Photo saveOne(UUID userId, PhotoSaveCommand command) {
    return photoRepository.save(
        new Photo(userId, command.coursePlaceId(), command.photoKey(), command.takenAt()));
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
