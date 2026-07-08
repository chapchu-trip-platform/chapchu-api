package com.pettrip.review.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.review.model.Review;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ReviewRepositoryTest {

  @Autowired private ReviewRepository reviewRepository;

  @Test
  void findByUserIdOrderByCreatedAtDesc는_해당_유저의_리뷰만_조회한다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    reviewRepository.save(new Review("place-1", ownerId, UUID.randomUUID(), (short) 5, "최고예요"));
    reviewRepository.save(new Review("place-2", otherId, UUID.randomUUID(), (short) 3, "그냥 그래요"));

    var result = reviewRepository.findByUserIdOrderByCreatedAtDesc(ownerId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContents()).isEqualTo("최고예요");
  }
}
