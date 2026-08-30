package com.pettrip.post.service;

import com.pettrip.post.controller.PostListResponse;
import com.pettrip.post.controller.PostResponse;
import com.pettrip.post.model.Post;
import com.pettrip.post.model.PostBookmark;
import com.pettrip.post.model.PostRecommendation;
import com.pettrip.post.model.PostReport;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRecommendationRepository;
import com.pettrip.post.repository.PostReportRepository;
import com.pettrip.post.repository.PostRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private static final String ENRICHED_SELECT =
      """
      SELECT p.post_id, p.user_id, p.pet_id, p.photo_id, p.course_id,
             p.title, p.content, p.view_count, p.recommendation_count, p.comment_count, p.created_at,
             COALESCE(u.nickname, '(탈퇴한 사용자)') AS nickname,
             ph.photo_url
      FROM posts p
      LEFT JOIN users u ON p.user_id = u.user_id
      LEFT JOIN photos ph ON p.photo_id = ph.photo_id
      """;

  private static final String LATEST_SQL =
      ENRICHED_SELECT + "ORDER BY p.created_at DESC, p.post_id DESC LIMIT :size";
  private static final String LATEST_CURSOR_SQL =
      ENRICHED_SELECT
          + "WHERE (p.created_at < :cursorAt OR (p.created_at = :cursorAt AND p.post_id < :cursorId)) "
          + "ORDER BY p.created_at DESC, p.post_id DESC LIMIT :size";
  private static final String POPULAR_SQL =
      ENRICHED_SELECT
          + "ORDER BY p.recommendation_count DESC, p.created_at DESC, p.post_id DESC LIMIT :size";
  private static final String DETAIL_SQL = ENRICHED_SELECT + "WHERE p.post_id = :postId";

  private static final RowMapper<PostResponse> POST_ROW_MAPPER =
      (rs, rowNum) ->
          new PostResponse(
              rs.getObject("post_id", UUID.class),
              rs.getObject("pet_id", UUID.class),
              rs.getObject("photo_id", UUID.class),
              rs.getObject("course_id", UUID.class),
              rs.getString("title"),
              rs.getString("content"),
              rs.getInt("view_count"),
              rs.getInt("recommendation_count"),
              rs.getInt("comment_count"),
              rs.getString("nickname"),
              rs.getString("photo_url"),
              rs.getTimestamp("created_at").toLocalDateTime());

  private final PostRepository postRepository;
  private final PostRecommendationRepository postRecommendationRepository;
  private final PostBookmarkRepository postBookmarkRepository;
  private final PostReportRepository postReportRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;

  public PostService(
      PostRepository postRepository,
      PostRecommendationRepository postRecommendationRepository,
      PostBookmarkRepository postBookmarkRepository,
      PostReportRepository postReportRepository,
      NamedParameterJdbcTemplate jdbcTemplate) {
    this.postRepository = postRepository;
    this.postRecommendationRepository = postRecommendationRepository;
    this.postBookmarkRepository = postBookmarkRepository;
    this.postReportRepository = postReportRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  public PostListResponse listPosts(String sort, String cursor, int size) {
    if ("popular".equals(sort)) {
      return queryPopular(size);
    }
    return queryLatest(cursor, size);
  }

  @Transactional
  public PostResponse getPost(UUID postId) {
    int updated = postRepository.incrementViewCount(postId);
    if (updated == 0) throw new PostNotFoundException();
    return fetchEnrichedPost(postId);
  }

  @Transactional
  public PostResponse createPost(
      UUID userId, UUID petId, UUID photoId, UUID courseId, String title, String content) {
    Post post = postRepository.save(new Post(userId, petId, photoId, courseId, title, content));
    return fetchEnrichedPost(post.getId());
  }

  @Transactional
  public PostResponse updatePost(UUID userId, UUID postId, String title, String content) {
    Post post = getOwnedPost(userId, postId);
    post.update(title, content);
    postRepository.save(post);
    return fetchEnrichedPost(postId);
  }

  public void deletePost(UUID userId, UUID postId) {
    Post post = getOwnedPost(userId, postId);
    postRepository.delete(post);
  }

  @Transactional
  public void recommend(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (postRecommendationRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostAlreadyRecommendedException();
    }
    postRecommendationRepository.save(new PostRecommendation(postId, userId));
    post.incrementRecommendationCount();
    postRepository.save(post);
  }

  @Transactional
  public void cancelRecommendation(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (!postRecommendationRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostRecommendationNotFoundException();
    }
    postRecommendationRepository.deleteByPostIdAndUserId(postId, userId);
    post.decrementRecommendationCount();
    postRepository.save(post);
  }

  public void bookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostAlreadyBookmarkedException();
    }
    postBookmarkRepository.save(new PostBookmark(userId, postId));
  }

  public void cancelBookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (!postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostBookmarkNotFoundException();
    }
    postBookmarkRepository.deleteByUserIdAndPostId(userId, postId);
  }

  public void report(UUID userId, UUID postId, String reportReason, String reportDetail) {
    findPost(postId);
    if (postReportRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostAlreadyReportedException();
    }
    postReportRepository.save(new PostReport(postId, userId, reportReason, reportDetail));
  }

  private PostListResponse queryLatest(String cursor, int size) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("size", size);
    if (cursor == null) {
      List<PostResponse> posts = jdbcTemplate.query(LATEST_SQL, params, POST_ROW_MAPPER);
      return toListResponse(posts, size);
    }
    String[] parts = cursor.split("~", 2);
    params.addValue(
        "cursorAt",
        Timestamp.valueOf(LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    params.addValue("cursorId", UUID.fromString(parts[1]));
    List<PostResponse> posts = jdbcTemplate.query(LATEST_CURSOR_SQL, params, POST_ROW_MAPPER);
    return toListResponse(posts, size);
  }

  private PostListResponse queryPopular(int size) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("size", size);
    List<PostResponse> posts = jdbcTemplate.query(POPULAR_SQL, params, POST_ROW_MAPPER);
    return new PostListResponse(posts, null);
  }

  private PostListResponse toListResponse(List<PostResponse> posts, int size) {
    if (posts.size() < size) {
      return new PostListResponse(posts, null);
    }
    PostResponse last = posts.get(posts.size() - 1);
    String nextCursor =
        last.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "~" + last.id();
    return new PostListResponse(posts, nextCursor);
  }

  private PostResponse fetchEnrichedPost(UUID postId) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", postId);
    return jdbcTemplate.query(DETAIL_SQL, params, POST_ROW_MAPPER).stream()
        .findFirst()
        .orElseThrow(PostNotFoundException::new);
  }

  private Post findPost(UUID postId) {
    return postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
  }

  private Post getOwnedPost(UUID userId, UUID postId) {
    Post post = findPost(postId);
    if (!post.getUserId().equals(userId)) {
      throw new PostNotFoundException();
    }
    return post;
  }
}
