package com.pettrip.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.common.service.InvalidReferenceException;
import com.pettrip.post.controller.PostResponse;
import com.pettrip.post.model.Post;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRecommendationRepository;
import com.pettrip.post.repository.PostReportRepository;
import com.pettrip.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock private PostRepository postRepository;
  @Mock private PostRecommendationRepository postRecommendationRepository;
  @Mock private PostBookmarkRepository postBookmarkRepository;
  @Mock private PostReportRepository postReportRepository;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private PostService postService;

  @BeforeEach
  void setUp() {
    postService =
        new PostService(
            postRepository,
            postRecommendationRepository,
            postBookmarkRepository,
            postReportRepository,
            jdbcTemplate);
  }

  private PostResponse samplePostResponse(UUID userId) {
    return new PostResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "제목",
        "내용",
        0,
        0,
        0,
        true,
        false,
        "닉네임",
        null,
        LocalDateTime.now());
  }

  @Test
  void getPost는_없으면_예외를_던진다() {
    UUID postId = UUID.randomUUID();
    when(postRepository.incrementViewCount(postId)).thenReturn(0);

    assertThatThrownBy(() -> postService.getPost(UUID.randomUUID(), postId))
        .isInstanceOf(PostNotFoundException.class);
  }

  @Test
  void getPost는_조회수를_증가시킨다() {
    UUID postId = UUID.randomUUID();
    PostResponse expected = samplePostResponse(UUID.randomUUID());
    when(postRepository.incrementViewCount(postId)).thenReturn(1);
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    PostResponse result = postService.getPost(UUID.randomUUID(), postId);

    assertThat(result).isEqualTo(expected);
    verify(postRepository).incrementViewCount(postId);
  }

  @Test
  void createPost는_게시글을_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID petId = UUID.randomUUID();
    UUID photoId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    PostResponse expected = samplePostResponse(userId);
    stubReferenceCheck(true, true, true);
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    PostResponse result = postService.createPost(userId, petId, photoId, courseId, "제목", "내용");

    assertThat(result).isEqualTo(expected);
  }

  private void stubReferenceCheck(boolean petOk, boolean photoOk, boolean courseOk) {
    when(jdbcTemplate.queryForObject(
            any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(new PostService.ReferenceCheck(petOk, photoOk, courseOk));
  }

  @Test
  void createPost는_photoId가_null이면_사진_검증을_건너뛴다() {
    UUID userId = UUID.randomUUID();
    PostResponse expected = samplePostResponse(userId);
    stubReferenceCheck(true, false, true);
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    PostResponse result =
        postService.createPost(userId, UUID.randomUUID(), null, UUID.randomUUID(), "제목", "내용");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void createPost는_photoId가_null이면_photoId를_쿼리_파라미터에서_뺀다() {
    UUID userId = UUID.randomUUID();
    stubReferenceCheck(true, false, true);
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.createPost(userId, UUID.randomUUID(), null, UUID.randomUUID(), "제목", "내용");

    ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
    verify(jdbcTemplate).queryForObject(any(String.class), captor.capture(), any(RowMapper.class));
    assertThat(captor.getValue().hasValue("photoId")).isFalse();
  }

  @Test
  void createPost는_petId와_courseId가_null이면_해당_검증을_건너뛴다() {
    UUID userId = UUID.randomUUID();
    PostResponse expected = samplePostResponse(userId);
    stubReferenceCheck(false, true, false);
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    PostResponse result =
        postService.createPost(userId, null, UUID.randomUUID(), null, "자유게시판 글", "내용");

    assertThat(result).isEqualTo(expected);
  }

  @Test
  void createPost는_참조가_모두_null이면_확인_쿼리를_돌리지_않는다() {
    UUID userId = UUID.randomUUID();
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.createPost(userId, null, null, null, "자유게시판 글", "내용");

    verify(jdbcTemplate, never())
        .queryForObject(any(String.class), any(SqlParameterSource.class), any(RowMapper.class));
  }

  @Test
  void createPost는_petId가_null이면_petId를_쿼리_파라미터에서_뺀다() {
    UUID userId = UUID.randomUUID();
    UUID courseId = UUID.randomUUID();
    stubReferenceCheck(false, false, true);
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.createPost(userId, null, null, courseId, "제목", "내용");

    ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
    verify(jdbcTemplate).queryForObject(any(String.class), captor.capture(), any(RowMapper.class));
    assertThat(captor.getValue().hasValue("petId")).isFalse();
    assertThat(captor.getValue().hasValue("courseId")).isTrue();
  }

  @Test
  void createPost는_저장을_flush한_뒤_되읽는다() {
    UUID userId = UUID.randomUUID();
    when(postRepository.saveAndFlush(any(Post.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.createPost(userId, null, null, null, "제목", "내용");

    InOrder inOrder = inOrder(postRepository, jdbcTemplate);
    inOrder.verify(postRepository).saveAndFlush(any(Post.class));
    inOrder
        .verify(jdbcTemplate)
        .query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class));
  }

  @Test
  void updatePost는_저장을_flush한_뒤_되읽는다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.updatePost(userId, postId, "새 제목", "새 내용");

    InOrder inOrder = inOrder(postRepository, jdbcTemplate);
    inOrder.verify(postRepository).saveAndFlush(post);
    inOrder
        .verify(jdbcTemplate)
        .query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class));
  }

  @Test
  void createPost는_남의_반려동물이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    stubReferenceCheck(false, true, true);

    assertThatThrownBy(
            () ->
                postService.createPost(
                    userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용"))
        .isInstanceOf(InvalidReferenceException.class)
        .extracting("field")
        .isEqualTo("petId");
  }

  @Test
  void createPost는_남의_사진이면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    stubReferenceCheck(true, false, true);

    assertThatThrownBy(
            () ->
                postService.createPost(
                    userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용"))
        .isInstanceOf(InvalidReferenceException.class)
        .extracting("field")
        .isEqualTo("photoId");
  }

  @Test
  void createPost는_남의_코스면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    stubReferenceCheck(true, true, false);

    assertThatThrownBy(
            () ->
                postService.createPost(
                    userId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용"))
        .isInstanceOf(InvalidReferenceException.class)
        .extracting("field")
        .isEqualTo("courseId");
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
  void cancelBookmark는_북마크를_삭제한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    Post post =
        new Post(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "제목", "내용");
    when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    when(postBookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

    postService.cancelBookmark(userId, postId);

    verify(postBookmarkRepository).deleteByUserIdAndPostId(userId, postId);
  }

  @Test
  void getPost는_요청한_사용자_기준으로_추천_북마크_여부를_조회한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    when(postRepository.incrementViewCount(postId)).thenReturn(1);
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(samplePostResponse(userId)));

    postService.getPost(userId, postId);

    ArgumentCaptor<SqlParameterSource> captor = ArgumentCaptor.forClass(SqlParameterSource.class);
    verify(jdbcTemplate).query(any(String.class), captor.capture(), any(RowMapper.class));
    assertThat(captor.getValue().getValue("userId")).isEqualTo(userId);
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
