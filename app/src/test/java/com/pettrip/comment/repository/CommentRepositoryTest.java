package com.pettrip.comment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pettrip.comment.model.Comment;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class CommentRepositoryTest {

  @Autowired private CommentRepository commentRepository;

  @Test
  void countByPostId는_해당_게시글의_댓글_수를_센다() {
    UUID postId = UUID.randomUUID();
    commentRepository.save(new Comment(postId, UUID.randomUUID(), null, 0, 1, "댓글1"));
    commentRepository.save(new Comment(postId, UUID.randomUUID(), null, 0, 2, "댓글2"));
    commentRepository.save(
        new Comment(UUID.randomUUID(), UUID.randomUUID(), null, 0, 1, "다른 글 댓글"));

    long count = commentRepository.countByPostId(postId);

    assertThat(count).isEqualTo(2);
  }
}
