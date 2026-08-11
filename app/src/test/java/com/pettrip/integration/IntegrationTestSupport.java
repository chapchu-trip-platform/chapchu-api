package com.pettrip.integration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 Postgres + Flyway + 전체 스프링 컨텍스트로 도는 통합 테스트의 공통 토대.
 *
 * <p>기존 테스트는 세 층으로 나뉘어 있고 층 사이가 비어 있다.
 *
 * <ul>
 *   <li>{@code @WebMvcTest} + 서비스 mock — HTTP 배선만
 *   <li>{@code @ExtendWith(MockitoExtension)} + 리포지토리 mock — 분기 로직만
 *   <li>{@code @DataJpaTest} + H2 (Flyway 꺼짐) — 쿼리 문법만
 * </ul>
 *
 * <p>그래서 FK, {@code ON DELETE CASCADE}, 시드 데이터, 도메인을 넘나드는 흐름은 어디서도 검증되지 않는다. 이 클래스는 그 틈을 메운다.
 *
 * <p>운영과 같은 {@code ddl-auto: validate}로 띄우므로, 엔티티 매핑이 Flyway 스키마와 어긋나면 컨텍스트 기동 자체가 실패한다.
 *
 * <p>{@code V1__init_schema.sql}이 {@code CREATE EXTENSION vector}를 하기 때문에 순정 postgres 이미지로는 안 되고
 * pgvector 이미지를 써야 한다.
 *
 * <p>Docker가 없는 환경(예: Docker Desktop이 꺼진 로컬)에서는 통째로 건너뛴다. CI(ubuntu-latest)에는 Docker가 있어 항상 돈다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(IntegrationTestSupport.TestJwtConfig.class)
@EnabledIf("dockerAvailable")
abstract class IntegrationTestSupport {

  private static PostgreSQLContainer<?> postgres;

  @Autowired protected TestRestTemplate rest;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired private JwtEncoder jwtEncoder;

  /** 컨테이너를 클래스 로딩 시점이 아니라 컨텍스트 준비 시점에 띄운다. Docker가 없으면 여기까지 오지 않는다. */
  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    if (postgres == null) {
      postgres =
          new PostgreSQLContainer<>(
              DockerImageName.parse("pgvector/pgvector:pg17")
                  .asCompatibleSubstituteFor("postgres"));
      postgres.start(); // JVM 종료 시 Ryuk이 정리한다. 테스트 클래스 간 재사용을 위해 stop 하지 않는다.
    }
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.flyway.enabled", () -> true);
    // 운영과 동일하게 검증만 한다. 스키마를 테스트가 만들어주면 매핑 불일치를 놓친다.
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

    // Spring Cloud AWS는 spring.cloud.aws.* 를 읽는다. application.yml이 이 값을
    // chapchu-api.cloud.aws.* 아래에 두고 있어 실제로는 아무 효과가 없다(운영에서는 k8s
    // 환경변수 AWS_REGION/AWS_ACCESS_KEY_ID 를 SDK 기본 체인이 주워서 우연히 동작한다).
    // 환경변수가 없는 테스트에서는 리전 해석이 실패해 컨텍스트가 뜨지 않으므로 여기서 채운다.
    // presigned URL 생성은 네트워크를 타지 않아 가짜 자격증명으로 충분하다.
    registry.add("spring.cloud.aws.region.static", () -> "ap-northeast-2");
    registry.add("spring.cloud.aws.credentials.access-key", () -> "test");
    registry.add("spring.cloud.aws.credentials.secret-key", () -> "test");

    // 슬라이스 테스트는 이 빈들을 만들지 않아 test application.yml에 값이 없다.
    // 전체 컨텍스트를 띄우는 여기서만 필요하다. 외부 TourAPI는 호출하지 않는다.
    registry.add("app.tour-api.key", () -> "test-key");
    registry.add("app.tour-api.base-url", () -> "http://localhost:1/never-called");
  }

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable t) {
      return false;
    }
  }

  // ─────────────────────────────────────────────── 인증

  /**
   * 인증 서버를 띄우는 대신 같은 키쌍으로 토큰을 발급하고 검증한다. 스프링 시큐리티 필터체인과 {@code @CurrentUserId} 해석은 운영과 똑같이 동작한다.
   */
  @TestConfiguration
  static class TestJwtConfig {

    @Bean
    RSAKey testRsaKey() {
      try {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey((RSAPrivateKey) pair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
      } catch (Exception e) {
        throw new IllegalStateException("테스트용 RSA 키를 만들지 못했다", e);
      }
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey key) throws Exception {
      return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey key) {
      return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
  }

  /** 운영 토큰과 같은 모양으로 만든다. {@code sub}가 {@code users.user_id}다. */
  protected String tokenFor(UUID userId) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(userId.toString())
            .issuer("http://localhost:9000")
            .audience(java.util.List.of("chapchu-api"))
            .issuedAt(now)
            .expiresAt(now.plus(30, ChronoUnit.MINUTES))
            .claim("scope", java.util.List.of("openid", "profile", "email"))
            .claim("role", "USER")
            .build();
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  // ─────────────────────────────────────────────── HTTP 도우미

  protected <T> ResponseEntity<T> get(String path, String token, Class<T> type) {
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers(token)), type);
  }

  protected <T> ResponseEntity<T> post(String path, String token, Object body, Class<T> type) {
    return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers(token)), type);
  }

  protected <T> ResponseEntity<T> patch(String path, String token, Object body, Class<T> type) {
    return rest.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body, headers(token)), type);
  }

  protected ResponseEntity<Void> delete(String path, String token) {
    return rest.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers(token)), Void.class);
  }

  private HttpHeaders headers(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  // ─────────────────────────────────────────────── 픽스처
  //
  // travel_courses / course_places 를 다루는 코드가 module-trip 에 아직 없다(소스 0개).
  // 그래서 게시글·사진의 선행 데이터는 SQL로 직접 넣는다. module-trip 이 생기면 API 호출로 바꾼다.

  /** auth 서버가 만드는 users 행을 흉내낸다. api 서버에는 유저 생성 경로가 없다. */
  protected UUID insertUser(String email, String nickname) {
    UUID userId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO users (user_id, google_user_id, email, nickname, role, account_status)"
            + " VALUES (?, ?, ?, ?, 'USER', 'ACTIVE')",
        userId,
        "google-" + userId,
        email,
        nickname);
    return userId;
  }

  protected UUID anyBreedId() {
    return jdbc.queryForObject(
        "SELECT breed_id FROM breeds ORDER BY breed_name LIMIT 1", UUID.class);
  }

  protected String insertPlace(String externalPlaceId, String placeName) {
    jdbc.update(
        "INSERT INTO places (external_place_id, place_name, address, latitude, longitude)"
            + " VALUES (?, ?, '서울시 어딘가', 37.5665, 126.9780)",
        externalPlaceId,
        placeName);
    return externalPlaceId;
  }

  protected UUID insertCourse(UUID userId) {
    UUID startCourseId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO start_course (start_course_id, start_course_location, start_course_time)"
            + " VALUES (?, '출발지', now())",
        startCourseId);
    UUID courseId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO travel_courses (course_id, user_id, start_course_id, travel_date)"
            + " VALUES (?, ?, ?, CURRENT_DATE)",
        courseId,
        userId,
        startCourseId);
    return courseId;
  }

  protected UUID insertCoursePlace(UUID courseId, String externalPlaceId) {
    UUID coursePlaceId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO course_places (course_place_id, course_id, external_place_id, visit_order)"
            + " VALUES (?, ?, ?, 1)",
        coursePlaceId,
        courseId,
        externalPlaceId);
    return coursePlaceId;
  }

  /** 리뷰 생성 API가 없어서(ReviewService에 create가 없다) SQL로 넣는다. */
  protected UUID insertReview(UUID userId, String placeId, UUID petId) {
    UUID reviewId = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO reviews (review_id, place_id, user_id, pet_id, rating, contents)"
            + " VALUES (?, ?, ?, ?, 5, '좋았어요')",
        reviewId,
        placeId,
        userId,
        petId);
    return reviewId;
  }

  protected long countRows(String table, String where, Object... args) {
    Long count =
        jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + where, Long.class, args);
    return count == null ? 0 : count;
  }
}
