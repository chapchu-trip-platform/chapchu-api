# AI 에이전트 실행 하네스 및 팀 협업 제약 조건 (AGENTS.md)

너는 현재 엄격한 통제 장치(Harness Engineering)가 적용된 Spring Boot 프로젝트에서 2명 이상의 인간 개발자와 협업하는 AI 에이전트다.
코드를 작성하거나 수정하기 전에 반드시 본 문서의 규칙을 파악하고, 실행 루프 내내 이를 준수해야 한다.

## ⚠️ 규칙 준수

- 이 파일의 모든 규칙은 예외 없이 따른다.
- 전역 `~/.claude/CLAUDE.md`와 충돌 시 이 파일(AGENTS.md)이 우선한다.
- **"이번만", "빨리", "일단"** 이유로 규칙을 생략하지 마라. 규칙을 바꾸고 싶으면 먼저 말하고 이 파일을 수정한 후 진행한다.

## 0. 세션 시작 시 필수 확인

1. 이 파일(AGENTS.md) 읽고 규칙 숙지
2. `docs/decisions/`, `docs/failures/` 최근 변경분 확인
3. 현재 브랜치 확인 (`git branch --show-current`)
4. 열린 PR 목록 확인 (`gh pr list`)
5. `git log --oneline -10`으로 최근 맥락 파악
6. `git pull` 직후라면 `.harness/last_pull_summary.md` 확인
7. 새 PR을 열 때마다 이 파일 다시 읽기

## 1. 아키텍처 제약 조건 (엄격한 계층형 구조)

- **도메인:** Java / Spring Boot 백엔드 아키텍처.
- **계층 규칙:** `ArchUnit`과 `Checkstyle`을 통해 단방향 의존성 흐름을 강제한다. (`Controller → Service → Repository`)
- `Domain` (Entity) 계층은 외부 계층을 절대 참조해서는 안 되는 순수한 핵심 비즈니스 영역이다. 의존성을 직접 주입하는 등 꼼수로 우회하지 마라.
- **Strict API/Backend Only:** 이 프로젝트는 FE가 완전히 분리된 순수 백엔드 API 서버다. 화면 렌더링 코드(HTML, CSS, JS, React, Thymeleaf 등)를 생성하거나 제안하지 마라. 단, `docs/` 목적의 정적 파일은 예외.

## 2. 지식 저장소 및 기술 결정 동기화

코드를 생성하기 전, 팀이 과거에 합의한 아키텍처 결정과 실패 경험을 반드시 숙지하라.

- **기술 결정 (`docs/decisions/`):** 팀이 선택한 합의안. 역행하는 기술을 도입하지 마라.
- **실패 기록 (`docs/failures/`):** 시도했다가 폐기된 안티패턴. 절대 제안하거나 코딩하지 마라.
- **기술 결정이 내려지면 즉시** `docs/decisions/`에 기록하라. 나중에 몰아서 하지 마라.
- **시도했다가 실패한 접근법은 즉시** `docs/failures/`에 기록하라.

## 3. 커밋 전 자동 검증 루프 (The Gauntlet)

코드를 완성하고 커밋을 시도하면 `pre-commit` 훅이 아래 명령어를 강제 실행한다:

```
./gradlew spotlessApply checkstyleMain spotbugsMain pmdMain test
```

빌드 실패(`Exit Code 1`) 발생 시, 반드시 아래의 **자율 수정 루프**를 수행하라:

1. **Spotless:** `./gradlew spotlessApply`를 실행하여 포맷을 다듬어라.
2. **Checkstyle:** 텍스트 로그를 읽고 잘못된 import나 네이밍을 수정하라.
3. **SpotBugs:** 잠재적 결함(NullPointer 등)을 고쳐라.
4. **PMD:** 사용하지 않는 변수나 프라이빗 메서드를 과감히 삭제하라.
5. **ArchUnit:** `AgentHarnessArchitectureTest` 또는 `DriftPreventionTest` 실패 시 에러 메시지를 읽고 수정하라.

빌드/테스트 실패 시 해결하기 전까지 다음 작업으로 넘어가지 마라.

## 4. 커밋 규칙

- 커밋 메시지는 한글로 작성한다. 형식: `{type}: {설명}` (type: feat / fix / chore / docs / refactor / test)
- **AI 서명 절대 금지**: `Co-Authored-By`, `Generated with Claude` 등 AI 크레딧을 커밋·PR에 넣지 마라.
- **하나의 PR = 하나의 커밋** 원칙. 추가 변경사항은 `git commit --amend`로 이전 커밋에 합친다.
- amend 후 push는 `git push --force-with-lease`로 한다.
- 커밋을 나누는 게 좋겠다고 판단되면 먼저 물어보고 결정한다.
- 예외: TDD 시 테스트 커밋 + 구현 커밋 2개 허용.
- **직접 `main` / `dev` push 금지.** 반드시 브랜치 → PR.

## 5. PR 규칙

- **항상 Draft로 먼저 올린다.** Ready for Review는 따로 지시할 때만 전환한다.
- **PR body 초안은 Claude가 채팅으로 작성해서 보여준다.** 실제 PR에 넣는 건 사용자가 직접 한다. (`gh pr create`를 Claude가 직접 실행하지 않는다.)
- 이슈가 있으면 body에 `Closes #N`을 반드시 포함한다.
- **이슈 번호 없는 브랜치/PR 금지.** 작업 전 이슈를 먼저 만들거나 확인하라.

## 6. 브랜치 규칙

- 형식: `{타입}/{기능명}` (예: `feat/nickname-change`, `fix/null-check`, `test/signup-flow`)
- 타입: `feat` / `fix` / `test` / `refactor` / `chore` / `docs`

## 7. 다자간 팀 협업 (Multi-User Collaboration Protocol)

### git pull 직후

1. `AGENTS.md`, `build.gradle`, `config/` 설정 파일과 `docs/decisions/`, `docs/failures/` 변경분을 최우선으로 확인하라.
2. 변경이 감지됐다면 즉시 해당 문서를 읽고 제약 조건을 재동기화하라.
3. `.harness/last_pull_summary.md` 및 `git log`로 동료의 최근 변경 맥락을 파악하라.

### 동료 코드 보호

현재 지시받은 과제와 직접 관련 없는 동료 코드를 '리팩토링' 명목으로 임의 수정하지 마라. 기존 퍼블릭 메서드 서명은 함부로 바꾸지 말고 안전하게 확장하라.

## 8. 엔티티 PK 정책 (UUID v7)

- 모든 Entity는 반드시 `BaseEntity`를 상속하라. 직접 `@Id` 필드를 선언하지 마라.
- `@GeneratedValue` 절대 금지. JPA 자동 생성 전략(IDENTITY, SEQUENCE, AUTO)을 사용하지 마라.
- `UUID.randomUUID()` 절대 금지 — B-Tree 인덱스 페이지 스플릿을 유발한다.
- UUID는 `uuid-creator` 라이브러리의 `UuidCreator.getTimeOrderedEpoch()`로 생성하며, `BaseEntity` 필드 초기화에서만 한다.
- DB DDL PK 컬럼: `UUID PRIMARY KEY DEFAULT uuid_generate_v7()` (pg_uuidv7 확장 필수).
- 예외: 코드값 테이블(breeds 등)은 `INT GENERATED ALWAYS AS IDENTITY`를 허용한다 (decisions/036 참고).

## 9. 패키지 명명 규칙

- 모든 클래스는 `com.pettrip.{domain}.{layer}` 2단계 하위 패키지에 위치시켜라.
  - 올바른 예: `com.pettrip.user.controller.UserController`
  - 잘못된 예: `com.pettrip.user.api.UserApi`, `com.pettrip.user.entity.User`
- 허용되는 레이어명: `controller` / `service` / `repository` / `model` 이 4가지뿐.
- `model` 패키지 클래스에서 `controller`, `service`, `repository` 패키지를 import하지 마라.
- 공통 클래스는 `com.pettrip.common.{layer}` 또는 `module-common` 모듈에 위치시켜라.

## 10. 기술 스택 제약

- **파일 저장**: 사진 URL은 S3 경로로 DB에 저장하라. 로컬 파일 경로를 저장하지 마라.
- **벡터 임베딩**: Spring AI `EmbeddingModel` 인터페이스를 사용하라. 임베딩 차원은 상수 `EMBEDDING_DIMENSION = 3072`로 관리하라. pgvector 컬럼은 `vector(3072)`으로 선언하라.
- **인증**: chapchu는 OAuth2 Resource Server 전용이다. 토큰 발급 로직을 이 레포에 추가하지 마라. 토큰 발급은 `chapchu-auth`(별도 레포) 전담이다.
- **Spring AI**: `module-recommendation/build.gradle`에 주석으로 보존된 Spring AI 의존성은 RAG 구현 착수 전 Maven Central에서 최신 GA 버전을 확인한 뒤 활성화하라. 지금 당장 활성화하지 마라.

## 11. 멀티모듈 의존성 제약

- `app` 모듈에 비즈니스 로직(Service, Repository, Entity)을 추가하지 마라. `app`은 조립 진입점이다.
- `module-recommendation`은 다른 도메인 모듈을 참조할 수 있지만, 역방향 의존성은 금지한다.
- 모듈 간 순환 의존성을 만들지 마라. ArchUnit이 빌드 시 감지한다.

## 12. 코드 스타일

- **if-else 금지** → early return으로 잘라서 읽기 쉽게 작성하라.
- **삼항 연산자 금지** → 조건이 중첩되면 가독성이 급격히 나빠진다.
- **`System.out.println` 금지** → 로거를 사용하거나 테스트에서는 assertion을 사용하라.
- **TODO 주석 금지** → 할 일이 있으면 이슈로 등록하라.
- **임시 코드 PR 포함 금지** → 디버깅용 코드, `*temp*`, `*_old.java`, `*.bak` 파일을 커밋하지 마라.
- **죽은 코드 제거:** 안 쓰는 메서드나 변수는 주석으로 남기지 말고 삭제하라.

## 13. 테스트 전략: TDD(Red-Green) + REST Docs

### TDD 순서 (필수)

실패하는 테스트 먼저 작성 → 구현 → 테스트 통과 확인

### Controller 테스트: @WebMvcTest + REST Docs

Controller를 구현하면 반드시 해당 엔드포인트의 MockMvc 테스트와 REST Docs 스니펫을 함께 작성한다.
테스트 없이 Controller를 구현하지 마라. 새 엔드포인트 추가 시 `app/src/docs/asciidoc/index.adoc`에도 추가한다.

### Service 테스트: 실제 DB 연동 (Testcontainers)

Service 클래스를 진입점으로 실제 PostgreSQL(Testcontainers)을 사용해 테스트한다.
Repository를 Mock하지 않는다. `Service → Repository → DB` 전부 함께 동작을 확인한다.

```java
// 올바른 예: ServiceTest with Testcontainers
@SpringBootTest
@Testcontainers
class UserServiceTest {
    @Container static PostgreSQLContainer<?> postgres = ...;
}
```

### 하지 않는 것

- Repository 단독 테스트 (`@DataJpaTest`) — Service 통합 테스트로 대체
- Mock으로 DB를 대체하는 Service 단위 테스트 — 실제 쿼리 버그를 잡지 못한다
- Swagger / SpringDoc — 프로덕션 코드에 어노테이션 침투 금지

## 14. DB 스키마 버전 관리 (Flyway)

- 스키마 변경 시 반드시 새 Flyway 마이그레이션 파일을 생성하라. DDL을 직접 DB에서 실행하지 마라.
- 파일 위치: `app/src/main/resources/db/migration/`
- 파일명 형식: `V{n}__{snake_case_설명}.sql`
- **기존 마이그레이션 파일을 절대 수정하지 마라.** Flyway는 체크섬으로 변조를 감지해 서버 기동을 막는다.
- `docs/schema/init.sql`은 전체 스키마 참고용 스냅샷이다. 마이그레이션 파일과 함께 최신 상태로 유지하라.
- `ddl-auto`는 항상 `validate`로 유지하라. `create`, `update`는 금지.

## 15. 확인 필요 목록

### ⛔ 절대 금지 (허락 없이 구현 안 함)

- DB 변경 (컬럼/테이블/Entity 필드 추가·삭제·변경)
- 되돌리기 어려운 작업 (force push, PR 닫기, 브랜치 삭제)

### ⚠️ 먼저 설명 후 진행

- 새 Java 파일/클래스 생성
- 기존 Entity·설정 파일 수정
- 새 라이브러리 추가 (`build.gradle` dependency)

## 16. 작업 범위 및 커뮤니케이션

### 요청한 것만 구현

요청한 것만 구현한다(테스트 포함). 범위 밖에서 개선할 점을 발견하면 먼저 구현하지 않고 설명한 뒤 물어본다.

### 모르면 물어보기

명확하고 명시적인 지시가 없으면 충분히 이해할 때까지 질문한다. 구현 방향이 확실하지 않으면 먼저 물어본다.
"일단 해보고 틀리면 고치기"는 하지 않는다.

### 에러 발생 시

수정하기 전에 원인부터 설명한다.

1. 일상 언어로 먼저: "어떤 파일에서 어떤 값이 없어서 터짐"
2. 필요하면 기술 용어도 함께: "`NullPointerException: X 필드가 null`"
3. "원인 → 해결방법" 순서로 설명한다.

### 선택지가 필요한 상황

쉬운 말로 3가지 옵션을 제시하고 대기한다.

```
A: DB에 컬럼 추가 → 기존 데이터 영향 없음
B ✅ 추천: 코드만 변경 → DB 그대로 유지 (이유: 지금 DB 변경은 위험 부담이 있어)
C: 작업 안 함 → 나중에 따로
```

기술 용어 없이 결과가 어떻게 달라지는지 설명하고, 추천 이유를 함께 표시하되 선택은 사용자가 한다.

### 작업 완료 후

바꾼 파일명 + 테스트 결과를 한 줄로 요약한다.

예: "완료. `AuthController.java` 수정 + `AuthControllerTest` 3개 통과."
