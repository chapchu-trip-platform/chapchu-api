# 047 — 게시글 분류: 일반 / 여행후기 2종

## 상태

- [x] 확정됨 (Accepted)

## 결정

게시글 종류를 `posts.post_type` 컬럼으로 명시 구분한다. 값은 두 개뿐이다.

| 값 | 뜻 |
|---|---|
| `GENERAL` | 일반 글 (기본값) |
| `TRAVEL_REVIEW` | 여행후기 |

- 컬럼: `VARCHAR(20) NOT NULL DEFAULT 'GENERAL'` (마이그레이션 V36)
- 엔티티: `@Enumerated(EnumType.STRING)` — 순서가 바뀌어도 값이 깨지지 않게 ORDINAL을 쓰지 않는다
- 생성 시 `postType`을 보내지 않으면 `GENERAL`로 저장한다
- 수정 시 `postType`을 보내지 않으면 기존 값을 유지한다(다른 필드와 같은 null=미변경 규칙)

## 목록 필터

`GET /posts?type=GENERAL|TRAVEL_REVIEW`. **생략하면 전체**를 돌려준다.

목록 조회는 이미 `sort`(latest/popular) × 커서 유무 조합이라, 타입 필터까지 더하면 경우의 수가 늘어난다.
SQL을 조합마다 통짜 상수로 두는 대신 조건절(`TYPE_PREDICATE`, `CURSOR_PREDICATE`,
`POPULAR_WINDOW_PREDICATE`)만 상수로 두고 `where(List<String>)`로 이어 붙인다.
조건이 하나도 없으면 `WHERE` 절 자체를 만들지 않는다.

타입 값은 `p.post_type = :postType` 바인딩 파라미터로만 넣는다. enum 이름을 SQL 문자열에 직접
이어 붙이지 않는다.

## 인덱스

V28이 만든 `posts(created_at)` 인덱스는 타입 조건이 붙으면 선택도가 떨어져 잘 쓰이지 않는다.
목록의 실제 접근 패턴(`post_type` 필터 + `created_at DESC, post_id DESC` 정렬)에 맞춰
복합 인덱스 `idx_posts_type_created_at`을 함께 만든다.

## 대안 (기각)

- **`is_review` boolean**: 종류가 셋 이상으로 늘면 컬럼을 또 만들어야 한다. 지금 2종이라도 enum이 낫다.
- **`Repository.findByPostType` 사용**: 목록 조회는 JPA가 아니라 JdbcTemplate 원시 SQL로
  닉네임·프로필사진·추천여부·사진수를 한 번에 조인해 가져온다. Repository 메서드를 더해도
  이 경로를 타지 않아 쓰이지 않는 코드가 된다.

## 에이전트 행동 지침

- 종류를 추가할 때는 enum 상수와 함께 `docs/schema/init.sql` 주석, 이 문서의 표를 같이 갱신하라.
- 목록 조회에 조건을 더할 때는 통짜 SQL 상수를 새로 만들지 말고 조건절 상수 + `where(...)` 방식을 따르라.
