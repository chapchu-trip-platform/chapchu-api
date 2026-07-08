package com.pettrip.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pettrip.comment.model.Comment;
import com.pettrip.comment.repository.CommentRepository;
import com.pettrip.post.repository.PostRepository;
import com.pettrip.post.service.PostNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock private CommentRepository commentRepository;
  @Mock private PostRepository postRepository;

  private CommentService commentService;

  @BeforeEach
  void setUp() {
    commentService = new CommentService(commentRepository, postRepository);
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
    when(commentRepository.save(any(Comment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Comment result = commentService.createComment(userId, postId, null, "내용");

    assertThat(result.getDepth()).isEqualTo(0);
    assertThat(result.getCommentOrder()).isEqualTo(1);
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
    when(commentRepository.save(any(Comment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Comment result = commentService.createComment(userId, postId, parentId, "대댓글");

    assertThat(result.getDepth()).isEqualTo(1);
    assertThat(result.getParentCommentId()).isEqualTo(parentId);
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
    UUID commentId = UUID.randomUUID();
    Comment comment = new Comment(UUID.randomUUID(), userId, null, 0, 1, "내용");
    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    commentService.deleteComment(userId, commentId);

    verify(commentRepository).delete(comment);
  }
}
