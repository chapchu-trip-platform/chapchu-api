package com.pettrip.post.service;

import com.pettrip.common.service.InvalidReferenceException;
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
             ph.photo_url,
             EXISTS(SELECT 1 FROM post_recommendations pr
                    WHERE pr.post_id = p.post_id AND pr.user_id = :userId) AS recommended,
             EXISTS(SELECT 1 FROM post_bookmarks pb
                    WHERE pb.post_id = p.post_id AND pb.user_id = :userId) AS bookmarked
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

  /**
   * 글이 가리키는 반려동물·사진·코스가 실제로 있고 작성자 것인지 한 번에 확인한다.
   *
   * <p>확인 없이 INSERT하면 FK 위반이 DataIntegrityViolationException으로 올라와 "입력 데이터가 올바르지 않습니다."라는 응답만 남는다.
   * 어느 값이 틀렸는지 알 수 없고, 남의 반려동물 ID를 붙여 글을 쓰는 것도 막지 못한다.
   */
  private static final String PET_EXISTS =
      "EXISTS(SELECT 1 FROM pets WHERE pet_id = :petId AND user_id = :userId)";

  private static final String PHOTO_EXISTS =
      "EXISTS(SELECT 1 FROM photos WHERE photo_id = :photoId AND user_id = :userId)";
  private static final String COURSE_EXISTS =
      "EXISTS(SELECT 1 FROM travel_courses WHERE course_id = :courseId AND user_id = :userId)";

  /** 참조 확인 쿼리 결과. 보내지 않은 참조는 항상 true다. */
  public record ReferenceCheck(boolean petOk, boolean photoOk, boolean courseOk) {}

  private static final RowMapper<ReferenceCheck> REFERENCE_CHECK_ROW_MAPPER =
      (rs, rowNum) ->
          new ReferenceCheck(
              rs.getBoolean("pet_ok"), rs.getBoolean("photo_ok"), rs.getBoolean("course_ok"));

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
              rs.getBoolean("recommended"),
              rs.getBoolean("bookmarked"),
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

  public PostListResponse listPosts(UUID userId, String sort, String cursor, int size) {
    if ("popular".equals(sort)) {
      return queryPopular(userId, size);
    }
    return queryLatest(userId, cursor, size);
  }

  @Transactional
  public PostResponse getPost(UUID userId, UUID postId) {
    int updated = postRepository.incrementViewCount(postId);
    if (updated == 0) throw new PostNotFoundException();
    return fetchEnrichedPost(userId, postId);
  }

  @Transactional
  public PostResponse createPost(
      UUID userId, UUID petId, UUID photoId, UUID courseId, String title, String content) {
    verifyReferences(userId, petId, photoId, courseId);
    Post post = postRepository.save(new Post(userId, petId, photoId, courseId, title, content));
    return fetchEnrichedPost(userId, post.getId());
  }

  @Transactional
  public PostResponse updatePost(UUID userId, UUID postId, String title, String content) {
    Post post = getOwnedPost(userId, postId);
    post.update(title, content);
    postRepository.save(post);
    return fetchEnrichedPost(userId, postId);
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

  @Transactional
  public void bookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostAlreadyBookmarkedException();
    }
    postBookmarkRepository.save(new PostBookmark(userId, postId));
  }

  @Transactional
  public void cancelBookmark(UUID userId, UUID postId) {
    findPost(postId);
    if (!postBookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
      throw new PostBookmarkNotFoundException();
    }
    postBookmarkRepository.deleteByUserIdAndPostId(userId, postId);
  }

  @Transactional
  public void report(UUID userId, UUID postId, String reportReason, String reportDetail) {
    findPost(postId);
    if (postReportRepository.existsByPostIdAndUserId(postId, userId)) {
      throw new PostAlreadyReportedException();
    }
    postReportRepository.save(new PostReport(postId, userId, reportReason, reportDetail));
  }

  private PostListResponse queryLatest(UUID userId, String cursor, int size) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("size", size);
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

  private PostListResponse queryPopular(UUID userId, int size) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("size", size);
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

  private PostResponse fetchEnrichedPost(UUID userId, UUID postId) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("postId", postId);
    return jdbcTemplate.query(DETAIL_SQL, params, POST_ROW_MAPPER).stream()
        .findFirst()
        .orElseThrow(PostNotFoundException::new);
  }

  private void verifyReferences(UUID userId, UUID petId, UUID photoId, UUID courseId) {
    if (petId == null && photoId == null && courseId == null) {
      return;
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    addReference(params, "petId", petId);
    addReference(params, "photoId", photoId);
    addReference(params, "courseId", courseId);

    String sql =
        "SELECT "
            + clause(petId, PET_EXISTS, "pet_ok")
            + ", "
            + clause(photoId, PHOTO_EXISTS, "photo_ok")
            + ", "
            + clause(courseId, COURSE_EXISTS, "course_ok");
    ReferenceCheck check = jdbcTemplate.queryForObject(sql, params, REFERENCE_CHECK_ROW_MAPPER);
    if (check == null) {
      throw new InvalidReferenceException("petId", "참조를 확인할 수 없습니다.");
    }
    if (petId != null && !check.petOk()) {
      throw new InvalidReferenceException("petId", "존재하지 않거나 본인의 반려동물이 아닙니다.");
    }
    if (photoId != null && !check.photoOk()) {
      throw new InvalidReferenceException("photoId", "존재하지 않거나 본인의 사진이 아닙니다.");
    }
    if (courseId != null && !check.courseOk()) {
      throw new InvalidReferenceException("courseId", "존재하지 않거나 본인의 여행 코스가 아닙니다.");
    }
  }

  /**
   * 보내지 않은 참조는 EXISTS 절 대신 {@code TRUE}를 넣는다.
   *
   * <p>null을 파라미터로 넘기면 Postgres가 {@code col = ?}의 타입을 추론하지 못해 쿼리가 실패할 수 있다. 절 자체를 빼는 편이 안전하다.
   */
  private static String clause(UUID id, String exists, String alias) {
    if (id == null) {
      return "TRUE AS " + alias;
    }
    return exists + " AS " + alias;
  }

  private static void addReference(MapSqlParameterSource params, String name, UUID id) {
    if (id == null) {
      return;
    }
    params.addValue(name, id);
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
