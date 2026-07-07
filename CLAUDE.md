# chapchu-api 프로젝트 AI 에이전트 필수 수칙

> 이 파일은 Claude Code가 세션 시작 시 자동으로 읽는다.
> 코드 한 줄 작성 전에 아래 체크리스트를 반드시 완료하라.

## 세션 시작 체크리스트 (매 세션 필수)

1. `docs/decisions/` 전체 파일을 읽고 기술 결정을 숙지하라.
2. `docs/failures/` 전체 파일을 읽고 안티패턴을 숙지하라.
3. `AGENTS.md` 전체를 읽고 하네스 규칙을 재확인하라.
4. `git log --oneline -10`으로 최근 변경 맥락을 파악하라.
5. git pull 직후라면 `.harness/last_pull_summary.md`를 확인하라.

## 결정·실패 기록 규칙 (절대 빠뜨리지 마라)

- 기술적 결정이 내려지면 **즉시** `docs/decisions/`에 기록하라. 나중에 하지 마라.
- 시도했다가 실패한 접근법은 **즉시** `docs/failures/`에 기록하라.
- 하네스 규칙에 영향을 주는 결정은 `AGENTS.md`에도 반영하라.
- 파일명 형식: `{번호}-{kebab-case-설명}.md` (예: `011-xxx.md`)

## 코드 작성 전 확인 (매 작업 필수)

- [ ] 레이어 규칙 준수: `Controller → Service → Repository → Model` 단방향
- [ ] 패키지 위치: `com.pettrip.{domain}.{controller|service|repository|model}`
- [ ] PK: `BaseEntity` 상속, `@GeneratedValue` 금지, `UUID.randomUUID()` 금지
- [ ] 새 Entity → `BaseEntity` 상속 확인
- [ ] 새 Controller → `@WebMvcTest` + REST Docs 스니펫 세트로 작성
- [ ] 모듈 의존성 방향 확인 (`app`에 비즈니스 로직 금지)

## 커밋 규칙

- 직접 `main` / `dev` push 금지. 반드시 `feature/{설명}` 브랜치 → PR
- 커밋 전 `./gradlew spotlessApply check` EXIT CODE 0 확인
- 커밋 메시지: `{type}: {설명}` (type: feat / fix / chore / docs / refactor / test)

## 참고 문서

- 상세 하네스 규칙: `AGENTS.md`
- 기술 결정 목록: `docs/decisions/`
- 실패 패턴 목록: `docs/failures/`
- API 명세 템플릿: `app/src/docs/asciidoc/index.adoc`
- DB 스키마: `docs/schema/init.sql`
