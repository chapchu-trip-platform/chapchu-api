package com.pettrip.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.post.model.Post;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PostRepositoryTest {

  @Autowired private PostRepository postRepository;

  @Test
  void findAllByOrderByCreatedAtDesc는_전체_게시글을_반환한다() {
    postRepository.save(
        new Post(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "첫 여행",
            "즐거웠어요"));

    var result = postRepository.findAllByOrderByCreatedAtDesc();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("첫 여행");
  }

  @Test
  void findByUserIdOrderByCreatedAtDesc는_해당_유저의_게시글만_반환한다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    postRepository.save(
        new Post(ownerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "내 글", "내용"));
    postRepository.save(
        new Post(otherId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "남의 글", "내용"));

    var result = postRepository.findByUserIdOrderByCreatedAtDesc(ownerId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("내 글");
  }
}
