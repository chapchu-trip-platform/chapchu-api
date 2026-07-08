package com.pettrip.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRecommendationRepository;
import com.pettrip.post.repository.PostReportRepository;
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
  @Mock private PostRecommendationRepository postRecommendationRepository;
  @Mock private PostBookmarkRepository postBookmarkRepository;
  @Mock private PostReportRepository postReportRepository;

  private PostService postService;

  @BeforeEach
  void setUp() {
    postService =
        new PostService(
            postRepository,
            postRecommendationRepository,
            postBookmarkRepository,
            postReportRepository);
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

  @Test
  void recommend는_이미_추천했으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postRecommendationRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

    assertThatThrownBy(() -> postService.recommend(userId, postId))
        .isInstanceOf(PostAlreadyRecommendedException.class);
  }

  @Test
  void recommend는_추천을_저장하고_카운트를_증가시킨다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postRecommendationRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);

    postService.recommend(userId, postId);

    assertThat(post.getRecommendationCount()).isEqualTo(1);
    verify(postRepository).save(post);
  }

  @Test
  void cancelRecommendation는_추천한_적이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postRecommendationRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);

    assertThatThrownBy(() -> postService.cancelRecommendation(userId, postId))
        .isInstanceOf(PostRecommendationNotFoundException.class);
  }

  @Test
  void bookmark는_이미_북마크했으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postBookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

    assertThatThrownBy(() -> postService.bookmark(userId, postId))
        .isInstanceOf(PostAlreadyBookmarkedException.class);
  }

  @Test
  void bookmark는_북마크를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postBookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);

    postService.bookmark(userId, postId);

    verify(postBookmarkRepository).save(any());
  }

  @Test
  void cancelBookmark는_북마크한_적이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postBookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);

    assertThatThrownBy(() -> postService.cancelBookmark(userId, postId))
        .isInstanceOf(PostBookmarkNotFoundException.class);
  }

  @Test
  void report는_이미_신고했으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postReportRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(true);

    assertThatThrownBy(() -> postService.report(userId, postId, "SPAM", "광고성 글입니다"))
        .isInstanceOf(PostAlreadyReportedException.class);
  }

  @Test
  void report는_신고를_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postReportRepository.existsByPostIdAndUserId(postId, userId)).thenReturn(false);

    postService.report(userId, postId, "SPAM", "광고성 글입니다");

    verify(postReportRepository).save(any());
  }
}
