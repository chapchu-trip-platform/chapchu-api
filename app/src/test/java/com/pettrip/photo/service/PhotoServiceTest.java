package com.pettrip.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.photo.model.Photo;
import com.pettrip.photo.model.PhotoType;
import com.pettrip.photo.repository.PhotoRepository;
import io.awspring.cloud.s3.S3Operations;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

  @Mock private PhotoRepository photoRepository;
  @Mock private S3Operations s3Operations;

  private PhotoService photoService;

  @BeforeEach
  void setUp() {
    photoService = new PhotoService(photoRepository, s3Operations, "test-bucket");
  }

  @Test
  void buildPhotoKey는_기능_유저ID_파일명을_포함한_경로를_생성한다() {
    UUID userId = UUID.randomUUID();

    String key = photoService.buildPhotoKey(userId, PhotoType.VISIT, "초코.jpg");

    assertThat(key).startsWith("visit/" + userId + "/").endsWith("-초코.jpg");
  }

  @Test
  void buildPhotoKey는_기능별로_다른_최상위_폴더를_쓴다() {
    UUID userId = UUID.randomUUID();

    assertThat(photoService.buildPhotoKey(userId, PhotoType.POST, "a.jpg")).startsWith("post/");
    assertThat(photoService.buildPhotoKey(userId, PhotoType.ALBUM, "a.jpg")).startsWith("album/");
  }

  @Test
  void issueUploadUrl은_S3Operations에_위임한다() throws Exception {
    String photoKey = "photos/user-1/uuid-초코.jpg";
    URL expectedUrl = URI.create("https://test-bucket.s3.amazonaws.com/" + photoKey).toURL();
    when(s3Operations.createSignedPutURL(eq("test-bucket"), eq(photoKey), any(Duration.class)))
        .thenReturn(expectedUrl);

    URL result = photoService.issueUploadUrl(photoKey);

    assertThat(result).isEqualTo(expectedUrl);
  }

  @Test
  void savePhoto는_사진을_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    String photoKey = "photos/user-1/uuid-초코.jpg";
    LocalDate takenAt = LocalDate.of(2026, 7, 1);
    when(photoRepository.save(any(Photo.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Photo result = photoService.savePhoto(userId, coursePlaceId, photoKey, takenAt);

    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getCoursePlaceId()).isEqualTo(coursePlaceId);
    assertThat(result.getPhotoUrl()).isEqualTo(photoKey);
    assertThat(result.getTakenAt()).isEqualTo(takenAt);
    verify(photoRepository).save(any(Photo.class));
  }

  @Test
  void getOwnedPhoto는_소유자면_사진을_반환한다() {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Photo photo = new Photo(userId, null, "visit/u/x-초코.jpg", LocalDate.of(2026, 7, 1));
    when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

    Photo result = photoService.getOwnedPhoto(userId, photoId);

    assertThat(result).isSameAs(photo);
  }

  @Test
  void getOwnedPhoto는_타인_사진이면_예외를_던진다() {
    UUID owner = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    Photo photo = new Photo(owner, null, "visit/u/x-초코.jpg", LocalDate.of(2026, 7, 1));
    when(photoRepository.findById(photoId)).thenReturn(Optional.of(photo));

    assertThatThrownBy(() -> photoService.getOwnedPhoto(other, photoId))
        .isInstanceOf(PhotoNotFoundException.class);
  }

  @Test
  void getOwnedPhoto는_없는_사진이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    when(photoRepository.findById(photoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> photoService.getOwnedPhoto(userId, photoId))
        .isInstanceOf(PhotoNotFoundException.class);
  }

  @Test
  void issueDownloadUrl은_S3Operations에_위임한다() throws Exception {
    String photoKey = "visit/user-1/uuid-초코.jpg";
    URL expectedUrl = URI.create("https://test-bucket.s3.amazonaws.com/" + photoKey).toURL();
    when(s3Operations.createSignedGetURL(eq("test-bucket"), eq(photoKey), any(Duration.class)))
        .thenReturn(expectedUrl);

    URL result = photoService.issueDownloadUrl(photoKey);

    assertThat(result).isEqualTo(expectedUrl);
  }
}
