# 005. PK 타입: UUID v7

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 모든 테이블 PK: `UUID` 타입 + `DEFAULT uuid_generate_v7()` (`pg_uuidv7` 확장)

## 이유
- UUID v4는 완전 랜덤이라 B-Tree 인덱스 페이지 스플릿 발생 → 쓰기 성능 저하, 인덱스 단편화
- UUID v7은 앞 48비트가 타임스탬프 → 거의 순차적으로 증가 → 페이지 스플릿 최소화
- BIGSERIAL 대비 UUID를 선택한 이유: API 노출 시 열거 공격 방지, 클라이언트 사전 ID 생성 가능

## 에이전트 행동 지침
- 모든 PK 컬럼은 `UUID PRIMARY KEY DEFAULT uuid_generate_v7()`으로 선언하라. (DB DEFAULT는 JPA 외부에서 직접 SQL 실행 시 안전망 역할)
- `uuid_generate_v4()` 또는 `gen_random_uuid()`는 사용하지 마라.
- `pg_uuidv7` 확장이 없는 환경에서는 실행 불가 — 로컬/운영 DB 초기화 시 반드시 확장 먼저 설치.
- JPA Entity에서는 `@GeneratedValue` 사용 금지. `uuid-creator` 라이브러리의 `UuidCreator.getTimeOrderedEpoch()`로 Java 레이어에서 생성하라.
- 모든 Entity는 `BaseEntity`를 상속받고, `BaseEntity`의 id 필드 초기화에서 UUID v7을 할당하라.
  ```java
  @Id
  private UUID id = UuidCreator.getTimeOrderedEpoch();
  ```
- 예외: 테이블에 `updated_at` 컬럼이 없는 경우 `BaseEntity`를 상속할 수 없다 ([[015-photo-entity-no-base-entity]] 참고). 이 경우에도 PK는 동일하게 `UuidCreator.getTimeOrderedEpoch()`로 직접 생성하라.
