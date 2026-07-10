package com.pettrip.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock private PostRepository postRepository;

  private PostService postService;

  @BeforeEach
  void setUp() {
    postService = new PostService(postRepository);
  }

  @Test
  void getPost는_없으면_예외를_던진다() {
    UUID postId = UUID.randomUUID();
    when(postRepository.findById(postId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> postService.getPost(postId)).isInstanceOf(PostNotFoundException.class);
  }

  @Test
  void getPost는_조회수를_증가시킨다() {
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Post result = postService.getPost(postId);

    assertThat(result.getViewCount()).isEqualTo(1);
  }

  @Test
  void createPost는_게시글을_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Post result = postService.createPost(userId, petId, photoId, courseId, "제목", "내용");

    assertThat(result.getTitle()).isEqualTo("제목");
    assertThat(result.getUserId()).isEqualTo(userId);
  }

  @Test
  void updatePost는_소유자가_아니면_예외를_던진다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(ownerId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));

    assertThatThrownBy(() -> postService.updatePost(otherId, postId, "새 제목", null))
        .isInstanceOf(PostNotFoundException.class);
  }

  @Test
  void deletePost는_소유한_게시글을_삭제한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));

    postService.deletePost(userId, postId);

    verify(postRepository).delete(post);
  }
}
