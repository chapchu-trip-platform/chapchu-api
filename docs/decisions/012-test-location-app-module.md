# 012. 테스트 코드 배치: 프로덕션은 도메인 모듈, 테스트는 app 모듈

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 프로덕션 코드(Controller/Service/Repository/Model)는 해당 도메인 모듈(`module-user`, `module-place` 등)에 둔다.
- `@WebMvcTest`/`@DataJpaTest`/순수 단위 테스트를 포함한 모든 테스트 코드는 `app/src/test/java/com/pettrip/{domain}/...`에 둔다.

## 이유
- REST Docs 스니펫 출력 경로(`app/build/generated-snippets`, decision 010)와 테스트 의존성(spring-boot-starter-test, spring-restdocs-mockmvc, h2, mockito 등)이 이미 `app/build.gradle`에만 배선되어 있다.
- 도메인 모듈의 `implementation` 의존성은 Gradle에서 소비 모듈의 컴파일 클래스패스로 전이되지 않으므로(`module-user`의 `spring-boot-starter-data-jpa` 등), `app`이 각 도메인 모듈 + 테스트 프레임워크를 모두 의존하는 유일한 지점이라 테스트를 여기 모으는 게 가장 마찰이 적다.
- `app`은 모든 도메인 모듈을 의존하므로(decision 007) 크로스 모듈 테스트가 자연스럽다.

## 에이전트 행동 지침
- 새 도메인 모듈(`module-place`, `module-trip` 등)에 Controller/Service/Repository를 추가할 때, 테스트는 그 모듈이 아니라 `app/src/test/java/com/pettrip/{도메인}/...`에 작성하라.
- 도메인 모듈에 `spring-boot-starter-test` 등 테스트 의존성을 추가하지 마라. 이미 `app`에 있다.
- Repository 테스트는 pgvector 등 PostgreSQL 전용 타입을 쓰지 않는 한 H2(`app`의 `testRuntimeOnly 'com.h2database:h2'`)로 충분하다. PostgreSQL 전용 기능이 필요해지면 Testcontainers 도입을 재검토하라.
- `app/src/test/resources/application.yml`은 `app/src/main/resources/application.yml`의 필수 환경변수(`${DB_URL}` 등)를 테스트용 더미 값으로 덮어쓴다. 새 환경변수가 메인 설정에 추가되면 이 파일에도 더미 값을 함께 추가하라.
- `app/src/main/resources/data.sql`은 `spring.sql.init.mode: always`(로컬 프로필 한정)일 때만 실행된다. 테스트(H2, 임베디드)에서는 `spring.sql.init.mode: never`로 꺼져 있으니, `data.sql`에 PostgreSQL 전용 함수(`uuid_generate_v7()` 등)를 추가해도 테스트가 깨지지 않는다 — 이 분리를 유지하라.
