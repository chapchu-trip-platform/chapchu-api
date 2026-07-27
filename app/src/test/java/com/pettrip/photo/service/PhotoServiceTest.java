package com.pettrip.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.photo.model.Photo;
import com.pettrip.photo.repository.PhotoRepository;
import io.awspring.cloud.s3.S3Operations;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
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
  void buildPhotoKey는_유저ID와_파일명을_포함한_경로를_생성한다() {
    UUID userId = UUID.randomUUID();

    String key = photoService.buildPhotoKey(userId, "초코.jpg");

    assertThat(key).startsWith("photos/" + userId + "/").endsWith("-초코.jpg");
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
}
