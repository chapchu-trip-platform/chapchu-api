package com.pettrip.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Postman 컬렉션에서 <b>선행 데이터를 만들 수 없어 건너뛰던 18개 요청</b>을 실제로 통과시킨다.
 *
 * <p>운영에서 건너뛴 이유는 각각 다음과 같았고, 여기서는 SQL 픽스처로 그 선행 조건을 직접 만든다.
 *
 * <ul>
 *   <li>반려동물 3건 — 품종 목록 API가 없어 {@code breedId}를 못 구함 → Flyway V2 시드에서 가져온다
 *   <li>게시글 6건 + 댓글 5건 — {@code courseId}를 만들 수 없음(module-trip 소스 0개) → SQL로 코스를 넣는다
 *   <li>리뷰 2건 — 리뷰 생성 API가 없음 → SQL로 리뷰를 넣는다
 *   <li>위시리스트 1건 — 추가 API가 없음 → SQL로 넣는다
 *   <li>장소 상세 1건 — TourAPI 키가 없어 목록이 500 → 장소를 SQL로 넣는다
 * </ul>
 */
class CommunityFlowIntegrationTest extends IntegrationTestSupport {

  private record Fixture(
      UUID userId, String token, UUID courseId, UUID coursePlaceId, String placeId) {}

  /** 한 유저와 그 유저의 코스·장소까지 갖춘 출발점을 만든다. */
  private Fixture newUser(String tag) {
    UUID userId = insertUser(tag + "@example.com", "닉네임" + tag);
    String placeId = insertPlace("PLACE-" + tag, "테스트장소" + tag);
    UUID courseId = insertCourse(userId);
    UUID coursePlaceId = insertCoursePlace(courseId, placeId);
    return new Fixture(userId, tokenFor(userId), courseId, coursePlaceId, placeId);
  }

  private UUID createPet(Fixture f, String name) {
    ResponseEntity<Map> res =
        post(
            "/pets",
            f.token(),
            Map.of("petName", name, "breedId", anyBreedId(), "size", "SMALL", "age", 3),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return UUID.fromString((String) res.getBody().get("id"));
  }

  private UUID createPhoto(Fixture f) {
    ResponseEntity<Map> res =
        post(
            "/photos",
            f.token(),
            Map.of(
                "coursePlaceId",
                f.coursePlaceId(),
                "photoKey",
                "photos/x/y.jpg",
                "takenAt",
                "2026-08-11"),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return UUID.fromString((String) res.getBody().get("id"));
  }

  private UUID createPost(Fixture f, String title) {
    ResponseEntity<Map> res =
        post(
            "/posts",
            f.token(),
            Map.of(
                "petId",
                createPet(f, "반려" + title),
                "photoId",
                createPhoto(f),
                "courseId",
                f.courseId(),
                "title",
                title,
                "content",
                "본문"),
            Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return UUID.fromString((String) res.getBody().get("id"));
  }

  // ─────────────────────────────────────────── 반려동물 (스킵 3건)

  @Test
  @DisplayName("반려동물을 등록하고 수정하고 삭제한다")
  void petLifecycle() {
    Fixture f = newUser("pet");

    UUID petId = createPet(f, "초코");

    ResponseEntity<List> list = get("/pets", f.token(), List.class);
    assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(list.getBody()).hasSize(1);

    ResponseEntity<Map> updated =
        patch("/pets/" + petId, f.token(), Map.of("age", 5, "size", "MEDIUM"), Map.class);
    assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updated.getBody().get("age")).isEqualTo(5);
    assertThat(updated.getBody().get("size")).isEqualTo("MEDIUM");
    assertThat(updated.getBody().get("breedName")).isNotNull();

    assertThat(delete("/pets/" + petId, f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(get("/pets", f.token(), List.class).getBody()).isEmpty();
  }

  @Test
  @DisplayName("남의 반려동물은 수정하거나 지울 수 없다")
  void cannotTouchOthersPet() {
    Fixture owner = newUser("owner");
    Fixture stranger = newUser("stranger");
    UUID petId = createPet(owner, "내강아지");

    assertThat(
            patch("/pets/" + petId, stranger.token(), Map.of("age", 9), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(delete("/pets/" + petId, stranger.token()).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  // ─────────────────────────────────────────── 사진

  @Test
  @DisplayName("업로드 URL을 발급받고 사진 메타데이터를 저장한다")
  void photoUploadFlow() {
    Fixture f = newUser("photo");

    ResponseEntity<Map> url =
        post("/photos/upload-url", f.token(), Map.of("fileName", "cat.jpg"), Map.class);
    assertThat(url.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    String photoKey = (String) url.getBody().get("photoKey");
    assertThat(photoKey).contains(f.userId().toString()).endsWith("cat.jpg");
    assertThat((String) url.getBody().get("uploadUrl")).startsWith("http");

    ResponseEntity<Map> saved =
        post(
            "/photos",
            f.token(),
            Map.of(
                "coursePlaceId", f.coursePlaceId(), "photoKey", photoKey, "takenAt", "2026-08-11"),
            Map.class);
    assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(saved.getBody().get("id")).isNotNull();
  }

  // ─────────────────────────────────────────── 게시글 (스킵 6건)

  @Test
  @DisplayName("글을 쓰면 목록에 뜨고 상세를 볼 때마다 조회수가 오른다")
  void postCreateAndView() {
    Fixture f = newUser("post");
    UUID postId = createPost(f, "첫 글");

    ResponseEntity<List> list = get("/posts?sort=latest", f.token(), List.class);
    assertThat(list.getBody()).isNotEmpty();

    ResponseEntity<Map> first = get("/posts/" + postId, f.token(), Map.class);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    int viewCount = (int) first.getBody().get("viewCount");

    ResponseEntity<Map> second = get("/posts/" + postId, f.token(), Map.class);
    assertThat((int) second.getBody().get("viewCount")).isEqualTo(viewCount + 1);
  }

  @Test
  @DisplayName("글을 수정하고 삭제한다. 남의 글은 건드릴 수 없다")
  void postUpdateAndDelete() {
    Fixture f = newUser("edit");
    Fixture stranger = newUser("editstranger");
    UUID postId = createPost(f, "수정 대상");

    ResponseEntity<Map> updated =
        patch("/posts/" + postId, f.token(), Map.of("title", "바뀐 제목"), Map.class);
    assertThat(updated.getBody().get("title")).isEqualTo("바뀐 제목");

    assertThat(
            patch("/posts/" + postId, stranger.token(), Map.of("title", "탈취"), Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(delete("/posts/" + postId, stranger.token()).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    assertThat(delete("/posts/" + postId, f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(get("/posts/" + postId, f.token(), Map.class).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("추천은 한 번만 되고, 취소하면 카운트가 돌아온다")
  void recommendOnce() {
    Fixture f = newUser("rec");
    UUID postId = createPost(f, "추천 대상");

    assertThat(
            post("/posts/" + postId + "/recommendations", f.token(), null, Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(
            (int)
                get("/posts/" + postId, f.token(), Map.class).getBody().get("recommendationCount"))
        .isEqualTo(1);

    // 두 번째 추천은 거부돼야 한다. PK (post_id, user_id)가 DB에서도 막는다.
    assertThat(
            post("/posts/" + postId + "/recommendations", f.token(), null, Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    assertThat(delete("/posts/" + postId + "/recommendations", f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(
            (int)
                get("/posts/" + postId, f.token(), Map.class).getBody().get("recommendationCount"))
        .isZero();
  }

  @Test
  @DisplayName("북마크하면 내 북마크 목록에 뜨고, 중복 북마크는 막힌다")
  void bookmarkFlow() {
    Fixture f = newUser("bm");
    UUID postId = createPost(f, "북마크 대상");

    assertThat(post("/posts/" + postId + "/bookmarks", f.token(), null, Void.class).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(get("/users/me/bookmarks", f.token(), List.class).getBody()).hasSize(1);

    assertThat(post("/posts/" + postId + "/bookmarks", f.token(), null, Void.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);

    assertThat(delete("/posts/" + postId + "/bookmarks", f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(get("/users/me/bookmarks", f.token(), List.class).getBody()).isEmpty();
  }

  @Test
  @DisplayName("같은 글을 두 번 신고할 수 없다")
  void reportOnce() {
    Fixture f = newUser("report");
    UUID postId = createPost(f, "신고 대상");
    Object body = Map.of("reportReason", "SPAM", "reportDetail", "광고");

    assertThat(post("/posts/" + postId + "/reports", f.token(), body, Void.class).getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(post("/posts/" + postId + "/reports", f.token(), body, Void.class).getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
  }

  // ─────────────────────────────────────────── 댓글 (스킵 5건)

  @Test
  @DisplayName("댓글과 대댓글을 달면 depth가 0과 1이다")
  void commentAndReply() {
    Fixture f = newUser("cmt");
    UUID postId = createPost(f, "댓글 대상");

    ResponseEntity<Map> comment =
        post("/posts/" + postId + "/comments", f.token(), Map.of("content", "첫 댓글"), Map.class);
    assertThat(comment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(comment.getBody().get("depth")).isEqualTo(0);
    UUID commentId = UUID.fromString((String) comment.getBody().get("id"));

    ResponseEntity<Map> reply =
        post(
            "/posts/" + postId + "/comments",
            f.token(),
            Map.of("parentCommentId", commentId, "content", "대댓글"),
            Map.class);
    assertThat(reply.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(reply.getBody().get("depth")).isEqualTo(1);

    assertThat(
            post("/posts/" + postId + "/comments", f.token(), Map.of("content", ""), Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);

    assertThat(delete("/comments/" + commentId, f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  @DisplayName("없는 글에는 댓글을 달 수 없고, 남의 댓글은 지울 수 없다")
  void commentGuards() {
    Fixture f = newUser("cmtguard");
    Fixture stranger = newUser("cmtstranger");
    UUID postId = createPost(f, "댓글 권한");

    assertThat(
            post(
                    "/posts/" + UUID.randomUUID() + "/comments",
                    f.token(),
                    Map.of("content", "유령"),
                    Map.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    UUID commentId =
        UUID.fromString(
            (String)
                post(
                        "/posts/" + postId + "/comments",
                        f.token(),
                        Map.of("content", "내 댓글"),
                        Map.class)
                    .getBody()
                    .get("id"));
    assertThat(delete("/comments/" + commentId, stranger.token()).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("글을 지우면 댓글도 함께 지워진다 (ON DELETE CASCADE)")
  void deletingPostCascadesComments() {
    Fixture f = newUser("cascade");
    UUID postId = createPost(f, "연쇄 삭제");
    post("/posts/" + postId + "/comments", f.token(), Map.of("content", "댓글"), Map.class);
    post("/posts/" + postId + "/bookmarks", f.token(), null, Void.class);

    assertThat(countRows("comments", "post_id = ?", postId)).isEqualTo(1);

    assertThat(delete("/posts/" + postId, f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);

    assertThat(countRows("comments", "post_id = ?", postId)).isZero();
    assertThat(countRows("post_bookmarks", "post_id = ?", postId)).isZero();
  }

  // ─────────────────────────────────────────── 리뷰 (스킵 2건)

  @Test
  @DisplayName("리뷰를 추천하고 취소한다")
  void reviewRecommendation() {
    Fixture f = newUser("review");
    UUID petId = createPet(f, "리뷰펫");
    UUID reviewId = insertReview(f.userId(), f.placeId(), petId);

    ResponseEntity<List> mine = get("/users/me/reviews", f.token(), List.class);
    assertThat(mine.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(mine.getBody()).hasSize(1);

    assertThat(
            post("/reviews/" + reviewId + "/recommendations", f.token(), null, Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CREATED);
    assertThat(
            post("/reviews/" + reviewId + "/recommendations", f.token(), null, Void.class)
                .getStatusCode())
        .isEqualTo(HttpStatus.CONFLICT);
    assertThat(delete("/reviews/" + reviewId + "/recommendations", f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
  }

  // ─────────────────────────────────────────── 위시리스트 (스킵 1건) · 장소 상세

  @Test
  @DisplayName("위시리스트를 조회하고 항목을 지운다")
  void wishlistFlow() {
    Fixture f = newUser("wish");
    // 위시리스트 추가 API가 없어 SQL로 넣는다.
    jdbc.update(
        "INSERT INTO place_wishlists (user_id, place_id) VALUES (?, ?)", f.userId(), f.placeId());

    ResponseEntity<List> list = get("/users/me/wishlist", f.token(), List.class);
    assertThat(list.getBody()).hasSize(1);

    assertThat(delete("/users/me/wishlist/" + f.placeId(), f.token()).getStatusCode())
        .isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(get("/users/me/wishlist", f.token(), List.class).getBody()).isEmpty();
  }

  @Test
  @DisplayName("장소 상세는 인증 없이도 볼 수 있다")
  void placeDetailIsPublic() {
    Fixture f = newUser("place");

    ResponseEntity<Map> res = get("/places/" + f.placeId(), null, Map.class);
    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody().get("placeName")).isEqualTo("테스트장소place");
  }

  // ─────────────────────────────────────────── 마이페이지

  @Test
  @DisplayName("글을 쓰면 홈과 마이페이지에 반영된다")
  void myPageReflectsData() {
    Fixture f = newUser("mypage");
    createPost(f, "마이페이지 글");

    assertThat(get("/users/me/posts", f.token(), List.class).getBody()).hasSize(1);

    ResponseEntity<Map> summary = get("/users/me/mypage", f.token(), Map.class);
    assertThat(summary.getBody().get("email")).isEqualTo("mypage@example.com");
    assertThat(((Number) summary.getBody().get("petCount")).intValue()).isEqualTo(1);

    ResponseEntity<Map> home = get("/home", f.token(), Map.class);
    assertThat(home.getBody().get("nickname")).isEqualTo("닉네임mypage");
    assertThat((List<?>) home.getBody().get("petNames")).hasSize(1);
  }

  // ─────────────────────────────────────────── 알려진 결함

  @Test
  @Disabled("createPost가 petId/photoId/courseId의 소유권을 검사하지 않는다. 수정되면 이 테스트를 켠다.")
  @DisplayName("남의 사진으로는 글을 쓸 수 없어야 한다")
  void cannotPostWithSomeoneElsesPhoto() {
    Fixture victim = newUser("victim");
    Fixture attacker = newUser("attacker");

    UUID victimPhotoId = createPhoto(victim);
    UUID attackerPetId = createPet(attacker, "공격자펫");

    ResponseEntity<Map> res =
        post(
            "/posts",
            attacker.token(),
            Map.of(
                "petId",
                attackerPetId,
                "photoId",
                victimPhotoId,
                "courseId",
                attacker.courseId(),
                "title",
                "탈취",
                "content",
                "남의 사진"),
            Map.class);

    assertThat(res.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
  }
}
