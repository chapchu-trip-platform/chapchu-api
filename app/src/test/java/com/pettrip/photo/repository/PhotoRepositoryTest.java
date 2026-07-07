package com.pettrip.photo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.common.config.JpaConfig;
import com.pettrip.photo.model.Photo;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(JpaConfig.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PhotoRepositoryTest {

  @Autowired private PhotoRepository photoRepository;

  @Test
  void 사진을_저장하고_조회한다() {
    UUID userId = UUID.randomUUID();
    UUID coursePlaceId = UUID.randomUUID();
    Photo saved =
        photoRepository.save(
            new Photo(
                userId, coursePlaceId, "photos/user-1/uuid-초코.jpg", LocalDate.of(2026, 7, 1)));

    Photo found = photoRepository.findById(saved.getId()).orElseThrow();

    assertThat(found.getUserId()).isEqualTo(userId);
    assertThat(found.getCoursePlaceId()).isEqualTo(coursePlaceId);
    assertThat(found.getPhotoUrl()).isEqualTo("photos/user-1/uuid-초코.jpg");
    assertThat(found.getCreatedAt()).isNotNull();
  }
}
