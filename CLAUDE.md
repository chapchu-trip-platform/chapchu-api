# chapchu-api 프로젝트 AI 에이전트 필수 수칙

> 이 파일은 Claude Code가 세션 시작 시 자동으로 읽는다.
> 코드 한 줄 작성 전에 아래 체크리스트를 반드시 완료하라.
> 전역 `~/.claude/CLAUDE.md`와 충돌 시 이 파일이 우선한다.

---

## 코스 생성 도메인 불변 규칙 (절대 어기지 마라)

> 아래 규칙은 `POST /courses` 코스 생성 플로우 전반에 적용된다.
> Opus 모델 검증 + 팀 합의로 확정된 결정이므로 "이번만", "빠르게" 이유로 우회 금지.

### TourAPI
- **반드시 `KorPetTourService2` 사용.** `KorService2` + `petTour=Y` 조합은 반려동물 전용 API가 아님 → 절대 금지
- base-url: `https://apis.data.go.kr/B551011/KorPetTourService2`
- `locationBasedList2` 호출 시 `petTour=Y` 파라미터 포함 금지. 이 서비스는 이미 전량 반려동물 데이터임
- 파싱 필수 필드: `contenttypeid`(카테고리), `dist`(거리), `mapx/mapy`(좌표), `firstimage`
- `detailPetTour2` 파싱 필수 필드: `acmpyTypeCd`(실내외), `acmpyPsblCpam`(가능견종/크기), `acmpyNeedMtr`(필수조건), `etcAcmpyInfo`

### 코스 생성 파라미터
- `POST /courses`에 `petId`는 **필수값(@NotNull)**. petId 없으면 코스 생성 불가 (400 반환)
- 날씨 정보(`temperature`, `humidity`, `weatherStatus`)는 **FE가 수집해서 요청에 포함**. 백엔드에서 기상청 API 직접 호출 금지
- 경로/지도 계산(거리, polyline)은 **FE가 처리**. 백엔드에서 카카오 Directions API 직접 호출 금지

### 장소 필터링
- 코스에 포함되는 장소는 **반드시 `place_pet_policies` 레코드가 있어야 함**
- `PlacePetPolicy.allowedPetSize` vs `Pet.size` 하드 필터 필수:
  - `AllowedPetSize.SMALL` 장소 → `PetSize.SMALL`만 입장 가능
  - `AllowedPetSize.MEDIUM` 장소 → `SMALL`, `MEDIUM` 입장 가능
  - `AllowedPetSize.LARGE` 또는 `ALL` → 전 견종 가능

### RAG / LLM
- `PlaceRagService` 고정 쿼리 `"반려동물 동반 즐거운 여행 좋은 장소"` 사용 금지
  → pet 정보 + 날씨 기반 동적 쿼리 생성 필수
- `RouteOptimizationService` LLM 프롬프트에 반드시 포함해야 할 컨텍스트:
  - 반려동물 크기/나이 요약
  - 날씨(맑음/비/흐림, 기온)
  - 각 장소의 `category`(음식점/관광지 등), `indoorOutdoor`(실내/실외/전구역)

### Flyway 마이그레이션 번호
- 현재 최신: **V22**. 신규 마이그레이션은 **V23부터** 시작할 것
- 마이그레이션 PR은 **머지 직전에** 번호 선점 여부를 다시 확인할 것 (failures/030)

---

## 세션 시작 체크리스트 (매 세션 필수)

1. `AGENTS.md` 전체를 읽고 하네스 규칙을 재확인하라.
2. `docs/decisions/` 전체 파일을 읽고 기술 결정을 숙지하라.
3. `docs/failures/` 전체 파일을 읽고 안티패턴을 숙지하라.
4. `git branch --show-current`로 현재 브랜치 확인.
5. `gh pr list`로 열린 PR 목록 확인.
6. `git log --oneline -10`으로 최근 변경 맥락을 파악하라.
7. git pull 직후라면 `.harness/last_pull_summary.md`를 확인하라.

## 결정·실패 기록 규칙 (절대 빠뜨리지 마라)

- 기술적 결정이 내려지면 **즉시** `docs/decisions/`에 기록하라. 나중에 하지 마라.
- 시도했다가 실패한 접근법은 **즉시** `docs/failures/`에 기록하라.
- 하네스 규칙에 영향을 주는 결정은 `AGENTS.md`에도 반영하라.
- 파일명 형식: `{번호}-{kebab-case-설명}.md` (예: `039-xxx.md`)

## 코드 작성 전 확인 (매 작업 필수)

- [ ] 관련 이슈 번호 확인 — 이슈 없으면 먼저 이슈 생성
- [ ] 레이어 규칙 준수: `Controller → Service → Repository → Model` 단방향
- [ ] 패키지 위치: `com.pettrip.{domain}.{controller|service|repository|model}`
- [ ] PK: `BaseEntity` 상속, `@GeneratedValue` 금지, `UUID.randomUUID()` 금지
- [ ] 새 Entity → `BaseEntity` 상속 확인 (코드값 테이블 예외: decisions/036)
- [ ] 새 Controller → `@WebMvcTest` + REST Docs 스니펫 세트로 작성
- [ ] 새 Service → Mockito 기반 단위 테스트 작성 (Repository Mock 허용)
- [ ] 모듈 의존성 방향 확인 (`app`에 비즈니스 로직 금지)
- [ ] if-else 없이 early return으로 작성
- [ ] 삼항 연산자 없이 작성

## 커밋·PR 규칙

- 직접 `main` / `dev` push 금지. 반드시 `{타입}/{기능명}` 브랜치 → PR
- 커밋 전 `./gradlew spotlessApply check` EXIT CODE 0 확인
- 커밋 메시지는 **한글**로: `{type}: {설명}` (type: feat / fix / chore / docs / refactor / test)
- **AI 서명 절대 금지**: Co-Authored-By, Generated with 등
- **하나의 PR = 하나의 커밋**. 추가 변경은 `git commit --amend` 후 `git push --force-with-lease`
- **PR은 항상 Draft로 먼저 생성.** PR body 초안은 Claude가 채팅으로 보여주고, 실제 등록은 사용자가 직접 함
- PR body에 `Closes #이슈번호` 반드시 포함

## 참고 문서

- 상세 하네스 규칙: `AGENTS.md`
- 기술 결정 목록: `docs/decisions/`
- 실패 패턴 목록: `docs/failures/`
- API 명세 템플릿: `app/src/docs/asciidoc/index.adoc`
- DB 스키마: `docs/schema/init.sql`
