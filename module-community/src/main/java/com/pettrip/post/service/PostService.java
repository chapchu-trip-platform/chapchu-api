package com.pettrip.post.service;

import com.github.f4b6a3.uuid.UuidCreator;
import com.pettrip.common.service.InvalidReferenceException;
import com.pettrip.post.controller.PostCreateRequest;
import com.pettrip.post.controller.PostListResponse;
import com.pettrip.post.controller.PostResponse;
import com.pettrip.post.controller.PostSummaryResponse;
import com.pettrip.post.model.Post;
import com.pettrip.post.model.PostBookmark;
import com.pettrip.post.model.PostRecommendation;
import com.pettrip.post.model.PostReport;
import com.pettrip.post.model.PostType;
import com.pettrip.post.repository.PostBookmarkRepository;
import com.pettrip.post.repository.PostRecommendationRepository;
import com.pettrip.post.repository.PostReportRepository;
import com.pettrip.post.repository.PostRepository;
import io.awspring.cloud.s3.S3Operations;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {

  private static final Duration DOWNLOAD_URL_DURATION = Duration.ofMinutes(10);

  private static final String ENRICHED_SELECT =
      """
      SELECT p.post_id, p.user_id, p.pet_id, p.photo_id, p.course_id, p.post_type,
             p.title, p.content, p.view_count, p.recommendation_count, p.comment_count, p.created_at,
             COALESCE(u.nickname, '(탈퇴한 사용자)') AS nickname,
             ph.photo_url,
             pf.photo_url AS profile_photo_url,
             EXISTS(SELECT 1 FROM post_recommendations pr
                    WHERE pr.post_id = p.post_id AND pr.user_id = :userId) AS recommended,
             EXISTS(SELECT 1 FROM post_bookmarks pb
                    WHERE pb.post_id = p.post_id AND pb.user_id = :userId) AS bookmarked,
             (SELECT count(*) FROM post_photos ppc
               WHERE ppc.post_id = p.post_id) AS photo_count
      FROM posts p
      LEFT JOIN users u ON p.user_id = u.user_id
      LEFT JOIN photos ph ON p.photo_id = ph.photo_id
      LEFT JOIN photos pf ON u.profile_photo_id = pf.photo_id
      """;

  /**
   * 목록 조회는 정렬(최신/인기) × 커서 유무 × 타입 필터 유무 조합이라 SQL을 통짜 상수로 두면 경우의 수만큼 문자열이 늘어난다. 조건절만 따로 두고 {@link
   * #where(List)}로 이어 붙인다.
   */
  private static final String LATEST_TAIL =
      "ORDER BY p.created_at DESC, p.post_id DESC LIMIT :size";

  private static final String POPULAR_TAIL =
      "ORDER BY p.recommendation_count DESC, p.created_at DESC, p.post_id DESC LIMIT :size";
  private static final String CURSOR_PREDICATE =
      "(p.created_at < :cursorAt OR (p.created_at = :cursorAt AND p.post_id < :cursorId))";
  private static final String POPULAR_WINDOW_PREDICATE =
      "p.created_at >= now() - INTERVAL '7 days'";
  private static final String TYPE_PREDICATE = "p.post_type = :postType";
  private static final String DETAIL_SQL = ENRICHED_SELECT + "WHERE p.post_id = :postId";

  /** 조건이 하나도 없으면 WHERE 절 자체를 빼고, 있으면 AND로 잇는다. */
  private static String where(List<String> predicates) {
    if (predicates.isEmpty()) {
      return "";
    }
    return "WHERE " + String.join(" AND ", predicates) + " ";
  }

  private static final String PHOTOS_BY_POST_SQL =
      """
      SELECT pp.post_id, pp.photo_id, ph.photo_url
      FROM post_photos pp
      JOIN photos ph ON ph.photo_id = pp.photo_id
      WHERE pp.post_id IN (:postIds)
      ORDER BY pp.post_id, pp.photo_order
      """;

  private static final String INSERT_PHOTO_SQL =
      """
      INSERT INTO photos (photo_id, user_id, course_place_id, photo_url, taken_at, created_at)
      VALUES (:photoId, :userId, NULL, :photoUrl, :takenAt, now())
      """;

  private static final String DELETE_POST_PHOTOS_SQL =
      "DELETE FROM post_photos WHERE post_id = :postId";

  private static final String INSERT_POST_PHOTO_SQL =
      """
      INSERT INTO post_photos (post_id, photo_id, photo_order)
      VALUES (:postId, :photoId, :photoOrder)
      """;

  /** 이미 있는 사진을 붙일 때, 그 photoId들이 존재하고 본인 것인지 확인한다. */
  private static final String OWNED_PHOTOS_SQL =
      "SELECT photo_id FROM photos WHERE photo_id IN (:photoIds) AND user_id = :userId";

  /**
   * 글이 가리키는 반려동물·사진·코스가 실제로 있고 작성자 것인지 한 번에 확인한다.
   *
   * <p>확인 없이 INSERT하면 FK 위반이 DataIntegrityViolationException으로 올라와 "입력 데이터가 올바르지 않습니다."라는 응답만 남는다.
   * 어느 값이 틀렸는지 알 수 없고, 남의 반려동물 ID를 붙여 글을 쓰는 것도 막지 못한다.
   */
  private static final String PET_EXISTS =
      "EXISTS(SELECT 1 FROM pets WHERE pet_id = :petId AND user_id = :userId)";

  private static final String COURSE_EXISTS =
      "EXISTS(SELECT 1 FROM travel_courses WHERE course_id = :courseId AND user_id = :userId)";

  /** 참조 확인 쿼리 결과. 보내지 않은 참조는 항상 true다. 사진은 photoKey 경로로 별도 검증한다. */
  public record ReferenceCheck(boolean petOk, boolean courseOk) {}

  private static final RowMapper<ReferenceCheck> REFERENCE_CHECK_ROW_MAPPER =
      (rs, rowNum) -> new ReferenceCheck(rs.getBoolean("pet_ok"), rs.getBoolean("course_ok"));

  private record PostPhotoRow(UUID postId, UUID photoId, String photoKey) {}

  private static final RowMapper<PostPhotoRow> POST_PHOTO_ROW_MAPPER =
      (rs, rowNum) ->
          new PostPhotoRow(
              rs.getObject("post_id", UUID.class),
              rs.getObject("photo_id", UUID.class),
              rs.getString("photo_url"));

  private final RowMapper<PostResponse> postRowMapper =
      (rs, rowNum) ->
          new PostResponse(
              rs.getObject("post_id", UUID.class),
              rs.getObject("pet_id", UUID.class),
              rs.getObject("photo_id", UUID.class),
              rs.getObject("course_id", UUID.class),
              PostType.valueOf(rs.getString("post_type")),
              rs.getString("title"),
              rs.getString("content"),
              rs.getInt("view_count"),
              rs.getInt("recommendation_count"),
              rs.getInt("comment_count"),
              rs.getBoolean("recommended"),
              rs.getBoolean("bookmarked"),
              rs.getString("nickname"),
              presign(rs.getString("profile_photo_url")),
              rs.getString("photo_url"),
              rs.getInt("photo_count"),
              List.of(),
              rs.getTimestamp("created_at").toLocalDateTime());

  private final RowMapper<PostSummaryResponse> postSummaryRowMapper =
      (rs, rowNum) ->
          new PostSummaryResponse(
              rs.getObject("post_id", UUID.class),
              PostType.valueOf(rs.getString("post_type")),
              rs.getString("title"),
              rs.getString("nickname"),
              presign(rs.getString("profile_photo_url")),
              rs.getInt("recommendation_count"),
              rs.getInt("comment_count"),
              rs.getInt("photo_count"),
              thumbnailOf(rs.getObject("photo_id", UUID.class), rs.getString("photo_url")),
              rs.getTimestamp("created_at").toLocalDateTime());

  /** 대표 사진(첫 장). photo_id가 없으면 사진 없는 글이라 null. */
  private PostResponse.PhotoView thumbnailOf(UUID photoId, String photoKey) {
    if (photoId == null) {
      return null;
    }
    return new PostResponse.PhotoView(photoId, photoKey, presign(photoKey));
  }

  /** S3 키를 presigned GET URL(문자열)로 바꾼다. 비공개 버킷이라 이 URL로만 열람 가능. 키가 없으면 null. */
  private String presign(String photoKey) {
    if (photoKey == null) {
      return null;
    }
    return s3Operations.createSignedGetURL(bucket, photoKey, DOWNLOAD_URL_DURATION).toString();
  }

  private final PostRepository postRepository;
  private final PostRecommendationRepository postRecommendationRepository;
  private final PostBookmarkRepository postBookmarkRepository;
  private final PostReportRepository postReportRepository;
  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final S3Operations s3Operations;
  private final String bucket;

  public PostService(
      PostRepository postRepository,
      PostRecommendationRepository postRecommendationRepository,
      PostBookmarkRepository postBookmarkRepository,
      PostReportRepository postReportRepository,
      NamedParameterJdbcTemplate jdbcTemplate,
      S3Operations s3Operations,
      @Value("${app.s3.bucket}") String bucket) {
    this.postRepository = postRepository;
    this.postRecommendationRepository = postRecommendationRepository;
    this.postBookmarkRepository = postBookmarkRepository;
    this.postReportRepository = postReportRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.s3Operations = s3Operations;
    this.bucket = bucket;
  }

  /**
   * 게시글 목록. {@code postType}이 null이면 종류를 가리지 않고 전부 돌려준다.
   *
   * @param sort {@code "popular"}면 최근 7일 추천순(커서 없음), 그 외에는 최신순 커서 페이지네이션
   */
  public PostListResponse listPosts(
      UUID userId, String sort, PostType postType, String cursor, int size) {
    if ("popular".equals(sort)) {
      return queryPopular(userId, postType, size);
    }
    return queryLatest(userId, postType, cursor, size);
  }

  @Transactional
  public PostResponse getPost(UUID userId, UUID postId) {
    int updated = postRepository.incrementViewCount(postId);
    if (updated == 0) throw new PostNotFoundException();
    return fetchEnrichedPost(userId, postId);
  }

  /**
   * 글을 저장한다. 응답은 없다(FE는 생성 후 목록으로 이동).
   *
   * <p>{@code saveAndFlush}로 INSERT를 강제하는 이유: 뒤이어 {@link #linkPostPhotos}가 JdbcTemplate 원시 SQL로
   * post_photos에 post_id FK를 넣는데, {@code Post}는 PK를 애플리케이션이 직접 만들어(@GeneratedValue 없음) {@code
   * save()}가 INSERT를 커밋 시점까지 미룬다. flush하지 않으면 아직 없는 글을 참조해 FK 위반이 난다.
   */
  @Transactional
  public void createPost(
      UUID userId,
      UUID petId,
      UUID courseId,
      PostType postType,
      String title,
      String content,
      List<PostCreateRequest.PhotoEntry> photos,
      List<UUID> existingPhotoIds) {
    verifyReferences(userId, petId, courseId);
    List<PostCreateRequest.PhotoEntry> entries = photos;
    if (entries == null) {
      entries = List.of();
    }
    // 앨범 등 이미 있는 사진(photoId)을 먼저 붙이고, 새로 올린 사진(photoKey)을 뒤에 잇는다.
    List<UUID> photoIds = new ArrayList<>(verifyOwnedPhotos(userId, existingPhotoIds));
    photoIds.addAll(createPhotos(userId, entries));
    Post post =
        postRepository.saveAndFlush(
            new Post(
                userId,
                petId,
                firstOrNull(photoIds),
                courseId,
                defaultType(postType),
                title,
                content));
    linkPostPhotos(post.getId(), photoIds);
  }

  /** 이미 있는 사진(photoId)들이 모두 존재하고 작성자 본인 것인지 확인한 뒤 중복을 제거해 순서대로 돌려준다. 하나라도 남의 사진이거나 없는 id면 거절한다. */
  private List<UUID> verifyOwnedPhotos(UUID userId, List<UUID> photoIds) {
    if (photoIds == null || photoIds.isEmpty()) {
      return List.of();
    }
    List<UUID> distinct = new ArrayList<>(new LinkedHashSet<>(photoIds));
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("photoIds", distinct).addValue("userId", userId);
    List<UUID> found = jdbcTemplate.queryForList(OWNED_PHOTOS_SQL, params, UUID.class);
    if (found.size() != distinct.size()) {
      throw new InvalidReferenceException("photoIds", "본인의 사진이 아니거나 존재하지 않습니다.");
    }
    return distinct;
  }

  /** photoKey들로 photos row를 만들고 생성된 photoId를 순서대로 돌려준다. */
  private List<UUID> createPhotos(UUID userId, List<PostCreateRequest.PhotoEntry> entries) {
    List<UUID> photoIds = new ArrayList<>();
    for (PostCreateRequest.PhotoEntry entry : entries) {
      verifyPhotoKeyOwnership(userId, entry.photoKey());
      UUID photoId = UuidCreator.getTimeOrderedEpoch();
      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("photoId", photoId)
              .addValue("userId", userId)
              .addValue("photoUrl", entry.photoKey())
              .addValue("takenAt", toSqlDate(entry.takenAt()), Types.DATE);
      jdbcTemplate.update(INSERT_PHOTO_SQL, params);
      photoIds.add(photoId);
    }
    return photoIds;
  }

  private void linkPostPhotos(UUID postId, List<UUID> photoIds) {
    for (int order = 0; order < photoIds.size(); order++) {
      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("postId", postId)
              .addValue("photoId", photoIds.get(order))
              .addValue("photoOrder", order);
      jdbcTemplate.update(INSERT_POST_PHOTO_SQL, params);
    }
  }

  /** photoKey는 {@code {type}/{userId}/...} 형식이라 두 번째 경로 조각이 본인 userId여야 한다. */
  private void verifyPhotoKeyOwnership(UUID userId, String photoKey) {
    String[] parts = photoKey.split("/");
    if (parts.length < 3 || !parts[1].equals(userId.toString())) {
      throw new InvalidReferenceException("photoKey", "본인의 사진 업로드 경로가 아닙니다.");
    }
  }

  private static UUID firstOrNull(List<UUID> photoIds) {
    if (photoIds.isEmpty()) {
      return null;
    }
    return photoIds.get(0);
  }

  private static Date toSqlDate(LocalDate takenAt) {
    if (takenAt == null) {
      return null;
    }
    return Date.valueOf(takenAt);
  }

  /**
   * 글을 수정한다. {@code photos}는 null이면 사진을 손대지 않고, 값이 오면 목록을 통째로 교체한다.
   *
   * <p>부분 삭제 대신 교체로 둔 이유: "3장 중 1장 빼기"를 하려면 어차피 남길 목록을 알아야 하고, 순서까지 프론트가 정할 수 있다. 빈 배열을 보내면 전부 뗀다.
   *
   * <p>{@code post_photos} 행만 지운다. {@code photos} 레코드와 S3 객체는 남는다. 같은 사진을 다른 글·리뷰가 참조할 수 있어서다.
   *
   * <p>{@code saveAndFlush}로 flush하는 이유는 {@link #createPost}와 같다. 하지 않으면 되읽을 때 수정 전 내용이 나간다.
   */
  @Transactional
  public PostResponse updatePost(
      UUID userId,
      UUID postId,
      PostType postType,
      String title,
      String content,
      List<PostCreateRequest.PhotoEntry> photos) {
    Post post = getOwnedPost(userId, postId);
    post.update(postType, title, content);
    if (photos != null) {
      replacePostPhotos(userId, post, photos);
    }
    postRepository.saveAndFlush(post);
    return fetchEnrichedPost(userId, postId);
  }

  private void replacePostPhotos(
      UUID userId, Post post, List<PostCreateRequest.PhotoEntry> photos) {
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("postId", post.getId());
    jdbcTemplate.update(DELETE_POST_PHOTOS_SQL, params);
    List<UUID> photoIds = createPhotos(userId, photos);
    linkPostPhotos(post.getId(), photoIds);
    post.replaceThumbnail(firstOrNull(photoIds));
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

  private PostListResponse queryLatest(UUID userId, PostType postType, String cursor, int size) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("size", size);
    List<String> predicates = new ArrayList<>();
    addTypeFilter(predicates, params, postType);
    if (cursor == null) {
      String sql = ENRICHED_SELECT + where(predicates) + LATEST_TAIL;
      return toListResponse(jdbcTemplate.query(sql, params, postSummaryRowMapper), size);
    }
    String[] parts = cursor.split("~", 2);
    params.addValue(
        "cursorAt",
        Timestamp.valueOf(LocalDateTime.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    params.addValue("cursorId", UUID.fromString(parts[1]));
    predicates.add(CURSOR_PREDICATE);
    String sql = ENRICHED_SELECT + where(predicates) + LATEST_TAIL;
    return toListResponse(jdbcTemplate.query(sql, params, postSummaryRowMapper), size);
  }

  private PostListResponse queryPopular(UUID userId, PostType postType, int size) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("size", size);
    List<String> predicates = new ArrayList<>();
    predicates.add(POPULAR_WINDOW_PREDICATE);
    addTypeFilter(predicates, params, postType);
    String sql = ENRICHED_SELECT + where(predicates) + POPULAR_TAIL;
    return new PostListResponse(jdbcTemplate.query(sql, params, postSummaryRowMapper), null);
  }

  /** 타입을 보내지 않은 글은 일반글로 저장한다. */
  private static PostType defaultType(PostType postType) {
    if (postType == null) {
      return PostType.GENERAL;
    }
    return postType;
  }

  /** 타입이 지정된 경우에만 조건과 바인딩 파라미터를 더한다. */
  private void addTypeFilter(
      List<String> predicates, MapSqlParameterSource params, PostType postType) {
    if (postType == null) {
      return;
    }
    predicates.add(TYPE_PREDICATE);
    params.addValue("postType", postType.name());
  }

  /** 상세 조회에 각 글의 사진 전체를 배치로 붙인다. post_photos → photos 순서대로. */
  private List<PostResponse> enrichWithPhotos(List<PostResponse> posts) {
    if (posts.isEmpty()) {
      return posts;
    }
    List<UUID> postIds = posts.stream().map(PostResponse::id).toList();
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("postIds", postIds);
    List<PostPhotoRow> rows = jdbcTemplate.query(PHOTOS_BY_POST_SQL, params, POST_PHOTO_ROW_MAPPER);

    Map<UUID, List<PostResponse.PhotoView>> byPost = new LinkedHashMap<>();
    for (PostPhotoRow row : rows) {
      byPost
          .computeIfAbsent(row.postId(), key -> new ArrayList<>())
          .add(new PostResponse.PhotoView(row.photoId(), row.photoKey(), presign(row.photoKey())));
    }
    return posts.stream()
        .map(post -> post.withPhotos(byPost.getOrDefault(post.id(), List.of())))
        .toList();
  }

  private PostListResponse toListResponse(List<PostSummaryResponse> posts, int size) {
    if (posts.size() < size) {
      return new PostListResponse(posts, null);
    }
    PostSummaryResponse last = posts.get(posts.size() - 1);
    String nextCursor =
        last.createdAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "~" + last.id();
    return new PostListResponse(posts, nextCursor);
  }

  private PostResponse fetchEnrichedPost(UUID userId, UUID postId) {
    MapSqlParameterSource params =
        new MapSqlParameterSource().addValue("userId", userId).addValue("postId", postId);
    PostResponse post =
        jdbcTemplate.query(DETAIL_SQL, params, postRowMapper).stream()
            .findFirst()
            .orElseThrow(PostNotFoundException::new);
    return enrichWithPhotos(List.of(post)).get(0);
  }

  private void verifyReferences(UUID userId, UUID petId, UUID courseId) {
    if (petId == null && courseId == null) {
      return;
    }
    MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
    addReference(params, "petId", petId);
    addReference(params, "courseId", courseId);

    String sql =
        "SELECT "
            + clause(petId, PET_EXISTS, "pet_ok")
            + ", "
            + clause(courseId, COURSE_EXISTS, "course_ok");
    ReferenceCheck check = jdbcTemplate.queryForObject(sql, params, REFERENCE_CHECK_ROW_MAPPER);
    if (check == null) {
      throw new InvalidReferenceException("petId", "참조를 확인할 수 없습니다.");
    }
    if (petId != null && !check.petOk()) {
      throw new InvalidReferenceException("petId", "존재하지 않거나 본인의 반려동물이 아닙니다.");
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
