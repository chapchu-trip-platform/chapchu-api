# 011. DB 스키마 버전 관리: Flyway

## 상태
- [x] 확정됨 (Accepted)

## 결정
- Flyway로 DB 스키마 변경 이력 관리
- 마이그레이션 파일 위치: `app/src/main/resources/db/migration/`
- 파일명 형식: `V{n}__{설명}.sql`

## 파일 구조
```
db/migration/
  V1__init_schema.sql   ← 초기 전체 스키마 (확장 설치 + 테이블 + 인덱스 + 코멘트)
  V2__xxx.sql           ← 이후 변경분만 기록
```

## 규칙
- 기존 마이그레이션 파일 수정 금지 (Flyway 체크섬 검증으로 서버 기동 실패)
- `ddl-auto: validate` 고정 — Flyway가 DDL 담당, JPA는 검증만
- `docs/schema/init.sql`은 전체 스키마 스냅샷으로 함께 유지

## 이유
- 스키마 변경 이력을 코드처럼 Git으로 추적
- 배포 시 Flyway가 자동으로 마이그레이션 실행 — 수동 DDL 실행 오류 방지
- 팀원 로컬 DB도 동일한 마이그레이션 순서로 초기화 가능

## 에이전트 행동 지침
- 스키마 변경 시 항상 새 `V{n}__` 파일 생성. 기존 파일 절대 수정 금지.
- `docs/schema/init.sql`도 함께 최신화하라.
- `ddl-auto: create` 또는 `update`로 변경하지 마라.
