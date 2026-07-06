# 010. 테스트 전략: TDD(Red-Green) + Spring REST Docs

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 개발 방식: **Red-Green TDD** — 실패 테스트 먼저 작성 후 구현
- API 명세: **Spring REST Docs** — 테스트 코드에서 문서 스니펫 자동 생성, Asciidoctor로 HTML 렌더링
- Swagger(SpringDoc) 미사용

## 테스트 레이어 전략
| 레이어 | 어노테이션 | 비고 |
|---|---|---|
| Controller | `@WebMvcTest` | MockMvc + REST Docs 스니펫 생성 |
| Service | 순수 JUnit 5 | Mockito로 Repository 모킹 |
| Repository | `@DataJpaTest` | 실제 H2/Testcontainers |

## REST Docs 산출물 위치
- 스니펫: `app/build/generated-snippets/`
- 최종 HTML: `app/build/docs/asciidoc/index.html`
- bootJar 포함 경로: `BOOT-INF/classes/static/docs/`
  → 배포 후 `/docs/index.html`로 접근 가능

## 이유
- REST Docs는 테스트가 통과해야 문서가 생성됨 → 명세와 구현 불일치 방지
- Swagger는 프로덕션 코드에 어노테이션이 침투하는 문제 있음
- TDD는 ArchUnit 계층 규칙과 자연스럽게 맞물림 (레이어별 테스트 분리)

## 에이전트 행동 지침
- 새 API 엔드포인트 구현 시 반드시 `@WebMvcTest` 테스트 + REST Docs 스니펫 생성 코드를 함께 작성하라.
- 테스트 없이 Controller를 구현하지 마라.
- `index.adoc`에 새 API 섹션 추가 시 해당 스니펫 include를 함께 추가하라.
