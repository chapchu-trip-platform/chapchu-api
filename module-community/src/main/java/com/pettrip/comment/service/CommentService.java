package com.pettrip.comment.service;

import com.pettrip.comment.controller.CommentResponse;
import com.pettrip.comment.model.Comment;
import com.pettrip.comment.repository.CommentRepository;
import com.pettrip.post.repository.PostRepository;
import com.pettrip.post.service.PostNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

  private static final String ENRICHED_SELECT =
      """
      SELECT c.comment_id, c.post_id, c.parent_comment_id, c.depth, c.comment_order,
             c.created_at,
             CASE WHEN c.deleted_at IS NULL THEN c.content ELSE '삭제된 댓글입니다' END AS content,
             CASE WHEN c.deleted_at IS NULL THEN COALESCE(u.nickname, '(탈퇴한 사용자)') END AS nickname,
             c.deleted_at IS NOT NULL AS deleted
      FROM comments c
      LEFT JOIN users u ON c.user_id = u.user_id
      """;

  private static final String LIST_SQL =
      ENRICHED_SELECT + "WHERE c.post_id = :postId ORDER BY c.comment_order ASC, c.created_at ASC";
  private static final String DETAIL_SQL = ENRICHED_SELECT + "WHERE c.comment_id = :commentId";

  private static final RowMapper<CommentResponse> COMMENT_ROW_MAPPER =
      (rs, rowNum) ->
          new CommentResponse(
              rs.getObject("comment_id", UUID.class),
              rs.getObject("post_id", UUID.class),
              rs.getObject("parent_comment_id", UUID.class),
              rs.getInt("depth"),
              rs.getInt("comment_order"),
              rs.getString("content"),
              rs.getString("nickname"),
              rs.getBoolean("deleted"),
              rs.getTimestamp("created_at").toLocalDateTime());

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public CommentService(
      CommentRepository commentRepository,
      PostRepository postRepository,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.commentRepository = commentRepository;
    this.postRepository = postRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<CommentResponse> listComments(UUID postId) {
    if (!postRepository.existsById(postId)) {
      throw new PostNotFoundException();
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", postId);
    return jdbcTemplate.query(LIST_SQL, params, COMMENT_ROW_MAPPER);
  }

  @Transactional
  public CommentResponse createComment(
      UUID userId, UUID postId, UUID parentCommentId, String content) {
    if (!postRepository.existsById(postId)) {
      throw new PostNotFoundException();
    }
    int depth = 0;
    if (parentCommentId != null) {
      Comment parent =
          commentRepository.findById(parentCommentId).orElseThrow(CommentNotFoundException::new);
      depth = parent.getDepth() + 1;
    }
    int order = (int) commentRepository.countByPostId(postId) + 1;
    Comment comment =
        commentRepository.saveAndFlush(
            new Comment(postId, userId, parentCommentId, depth, order, content));
    postRepository.incrementCommentCount(postId);
    return fetchEnrichedComment(comment.getId());
  }

  @Transactional
  public CommentResponse updateComment(UUID userId, UUID commentId, String content) {
    Comment comment = getOwnedComment(userId, commentId);
    comment.update(content);
    commentRepository.saveAndFlush(comment);
    return fetchEnrichedComment(commentId);
  }

  /**
   * 행을 지우지 않고 삭제 표시만 남긴다. {@code comments.parent_comment_id}가 ON DELETE CASCADE라 하드 삭제하면 부모 댓글 하나로
   * 대댓글 전체가 함께 사라진다.
   */
  @Transactional
  public void deleteComment(UUID userId, UUID commentId) {
    Comment comment = getOwnedComment(userId, commentId);
    comment.softDelete(LocalDateTime.now());
    commentRepository.saveAndFlush(comment);
    postRepository.decrementCommentCount(comment.getPostId());
  }

  private CommentResponse fetchEnrichedComment(UUID commentId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("commentId", commentId);
    return jdbcTemplate.query(DETAIL_SQL, params, COMMENT_ROW_MAPPER).stream()
        .findFirst()
        .orElseThrow(CommentNotFoundException::new);
  }

  private Comment getOwnedComment(UUID userId, UUID commentId) {
    Comment comment =
        commentRepository.findById(commentId).orElseThrow(CommentNotFoundException::new);
    if (!userId.equals(comment.getUserId()) || comment.isDeleted()) {
      throw new CommentNotFoundException();
    }
    return comment;
  }
}
