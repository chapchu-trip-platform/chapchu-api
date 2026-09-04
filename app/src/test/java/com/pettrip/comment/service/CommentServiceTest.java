package com.pettrip.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.comment.controller.CommentResponse;
import com.pettrip.comment.model.Comment;
import com.pettrip.comment.repository.CommentRepository;
import com.pettrip.post.repository.PostRepository;
import com.pettrip.post.service.PostNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock private CommentRepository commentRepository;
  @Mock private PostRepository postRepository;
  @Mock private NamedParameterJdbcTemplate jdbcTemplate;

  private CommentService commentService;

  @BeforeEach
  void setUp() {
    commentService = new CommentService(commentRepository, postRepository, jdbcTemplate);
  }

  private CommentResponse sampleResponse(String content) {
    return new CommentResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        null,
        0,
        1,
        content,
        "밤톨이아빠",
        LocalDateTime.of(2024, 1, 15, 10, 30, 0));
  }

  private void stubEnrichedQuery(CommentResponse response) {
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(response));
  }

  @Test
  void createComment는_게시글이_없으면_예외를_던진다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    when(postRepository.existsById(postId)).thenReturn(false);

    assertThatThrownBy(() -> commentService.createComment(userId, postId, null, "내용"))
        .isInstanceOf(PostNotFoundException.class);
  }

  @Test
  void createComment는_부모가_없으면_depth0으로_저장한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.countByPostId(postId)).thenReturn(0L);
    when(commentRepository.saveAndFlush(any(Comment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubEnrichedQuery(sampleResponse("내용"));

    commentService.createComment(userId, postId, null, "내용");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getDepth()).isEqualTo(0);
    assertThat(captor.getValue().getCommentOrder()).isEqualTo(1);
  }

  @Test
  void createComment는_부모가_있으면_부모_depth1을_더한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    Comment parent = new Comment(postId, UUID.randomUUID(), null, 0, 1, "부모 댓글");
    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(commentRepository.countByPostId(postId)).thenReturn(1L);
    when(commentRepository.saveAndFlush(any(Comment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    stubEnrichedQuery(sampleResponse("대댓글"));

    commentService.createComment(userId, postId, parentId, "대댓글");

    ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
    verify(commentRepository).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getDepth()).isEqualTo(1);
    assertThat(captor.getValue().getParentCommentId()).isEqualTo(parentId);
  }

  @Test
  void listComments는_게시글이_없으면_예외를_던진다() {
    UUID postId = UUID.randomUUID();
    when(postRepository.existsById(postId)).thenReturn(false);

    assertThatThrownBy(() -> commentService.listComments(postId))
        .isInstanceOf(PostNotFoundException.class);
  }

  @Test
  void listComments는_해당_게시글의_댓글을_돌려준다() {
    UUID postId = UUID.randomUUID();
    CommentResponse expected = sampleResponse("좋은 글이네요");
    when(postRepository.existsById(postId)).thenReturn(true);
    when(jdbcTemplate.query(any(String.class), any(SqlParameterSource.class), any(RowMapper.class)))
        .thenReturn(List.of(expected));

    List<CommentResponse> result = commentService.listComments(postId);

    assertThat(result).containsExactly(expected);
  }

  @Test
  void updateComment는_소유자가_아니면_예외를_던진다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    Comment comment = new Comment(UUID.randomUUID(), ownerId, null, 0, 1, "내용");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentService.updateComment(otherId, commentId, "고친 내용"))
        .isInstanceOf(CommentNotFoundException.class);
  }

  @Test
  void updateComment는_소유한_댓글의_내용을_바꾼다() {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    Comment comment = new Comment(UUID.randomUUID(), userId, null, 0, 1, "내용");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    stubEnrichedQuery(sampleResponse("고친 내용"));

    CommentResponse result = commentService.updateComment(userId, commentId, "고친 내용");

    assertThat(comment.getContent()).isEqualTo("고친 내용");
    assertThat(result.content()).isEqualTo("고친 내용");
    verify(commentRepository).saveAndFlush(comment);
  }

  @Test
  void deleteComment는_소유자가_아니면_예외를_던진다() {
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    Comment comment = new Comment(UUID.randomUUID(), ownerId, null, 0, 1, "내용");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    assertThatThrownBy(() -> commentService.deleteComment(otherId, commentId))
        .isInstanceOf(CommentNotFoundException.class);
  }

  @Test
  void deleteComment는_소유한_댓글을_삭제한다() {
    UUID userId = UUID.randomUUID();
    UUID postId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    Comment comment = new Comment(postId, userId, null, 0, 1, "내용");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    commentService.deleteComment(userId, commentId);

    verify(commentRepository).delete(comment);
    verify(postRepository).decrementCommentCount(postId);
  }
}
