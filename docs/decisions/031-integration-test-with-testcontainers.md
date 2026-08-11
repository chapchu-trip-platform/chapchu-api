# 031. 통합 테스트는 Testcontainers + pgvector 이미지로 실제 Postgres에 붙인다

## 배경

기존 테스트 43개는 세 층으로 깔끔하게 나뉘어 있었다.

| 층 | 방식 | 검증 범위 |
|---|---|---|
| 컨트롤러 21개 | `@WebMvcTest` + 서비스 mock | HTTP 배선·직렬화·검증 메시지 |
| 서비스 13개 | Mockito + 리포지토리 mock | 분기 로직 |
| 리포지토리 8개 | `@DataJpaTest` + H2 | 쿼리 문법 |

**세 층을 이어 붙인 테스트가 하나도 없었다.** 게다가 테스트는 H2에 `ddl-auto: create-drop`,
Flyway는 꺼져 있어서 **실제 Postgres 스키마(V1~V6)가 한 번도 실행되지 않았다.**

그래서 다음이 어디서도 검증되지 않았다.

- 트랜잭션 경계 (`docs/failures/025` — 실제로 여기서 버그가 났다)
- FK와 `ON DELETE CASCADE`
- Flyway 시드 데이터 (`breeds`, 선호 코드)
- 도메인을 넘나드는 흐름 (반려동물 → 사진 → 글 → 댓글)
- 엔티티 매핑과 실제 스키마의 일치

## 결정

`@SpringBootTest(RANDOM_PORT)` + Testcontainers로 **실제 Postgres**를 띄우고 Flyway를 돌린다.
공통 토대는 `app/src/test/java/com/pettrip/integration/IntegrationTestSupport.java`.

### pgvector 이미지를 써야 한다

`V1__init_schema.sql`이 `CREATE EXTENSION IF NOT EXISTS vector`를 한다. 순정 `postgres` 이미지에는
pgvector가 없어 마이그레이션이 실패한다.

```java
new PostgreSQLContainer<>(
    DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));
```

### ddl-auto는 운영과 같은 validate

스키마를 Hibernate가 만들어주면 매핑 불일치를 놓친다. `validate`로 두면 엔티티가 Flyway 스키마와
어긋날 때 **컨텍스트 기동 자체가 실패**한다.

### 인증 서버를 띄우지 않는다

테스트 안에서 RSA 키쌍을 만들어 `JwtEncoder`로 토큰을 발급하고 같은 키로 `JwtDecoder`를 만든다.
스프링 시큐리티 필터체인과 `@CurrentUserId` 해석은 운영과 똑같이 돈다.

### Testcontainers 버전을 덮어쓴다

Boot 3.4.5가 고정하는 1.20.6은 최신 Docker Desktop의 소켓을 인식하지 못한다
(`UnixSocketClientProviderStrategy`가 400, `DockerDesktopClientProviderStrategy`가 NPE).

```groovy
ext['testcontainers.version'] = '1.21.4'   // BOM을 덧붙이면 Boot 쪽이 이긴다
```

### Docker가 없으면 건너뛴다

`@EnabledIf("dockerAvailable")`. 로컬에서 Docker Desktop을 안 켠 사람도 `./gradlew check`가 돌아간다.
CI(ubuntu-latest)에는 Docker가 있으므로 **항상 실행된다.**

## module-trip이 빌 동안의 픽스처

`travel_courses` / `course_places`를 다루는 코드가 아직 없다(module-trip 소스 0개). 게시글은 `courseId`가
필수라서 API만으로는 아무것도 만들 수 없다. 그래서 선행 데이터는 `JdbcTemplate`으로 직접 넣는다.
**module-trip이 생기면 그 부분을 API 호출로 바꾼다.**

리뷰 생성 API와 위시리스트 추가 API도 없어 같은 방식으로 넣는다.

## 성과

첫 실행에서 **운영 버그 1건**(`docs/failures/025`)과 **죽은 설정 1건**을 잡았다.

죽은 설정: `application.yml`이 AWS 설정을 `chapchu-api.cloud.aws.*` 아래에 두는데 Spring Cloud AWS는
`spring.cloud.aws.*`를 읽는다. 운영에서는 k8s 환경변수를 SDK 기본 체인이 주워서 **우연히** 동작하고 있다.
이 블록은 아무 효과가 없다. 별도로 정리해야 한다.

## 에이전트 행동 지침

- 새 도메인 흐름을 만들면 슬라이스 테스트만으로 끝내지 마라. **층 사이는 통합 테스트만 본다.**
- 통합 테스트는 `IntegrationTestSupport`를 상속하고 픽스처 헬퍼를 재사용하라.
- 테스트 실행 시간이 아깝다고 `@DataJpaTest`로 대체하지 마라. 그건 트랜잭션을 대신 걸어주므로
  트랜잭션 버그를 **가린다.**
