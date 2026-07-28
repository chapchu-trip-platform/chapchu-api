# 021. PostgreSQL 이미지: pgvector/pgvector:pg16

## 상태
확정 (2026-07-28)

## 결정
PostgreSQL 이미지로 **`pgvector/pgvector:pg16`** 사용.

## 이유
- `vector` 타입과 관련 함수(`<=>` 코사인 유사도 등)가 기본 포함
- `postgres:16` 공식 이미지는 pgvector 확장 없음 → `CREATE EXTENSION vector` 실패
- `pg_uuidv7` 확장은 `pgvector/pgvector:pg16`에도 포함되지 않음 → V1 마이그레이션에서 `uuid_generate_v7()` 대신 `gen_random_uuid()` 사용 (decisions/005 참조)

## UUID 전략
DB 레벨: `gen_random_uuid()` (PostgreSQL 내장, UUID v4)
애플리케이션 레벨: `uuid-creator:5.3.7` 라이브러리로 UUID v7 생성 (decisions/005 참조)

## k8s 배포
`k8s/postgres.yml` — PVC 5Gi + Deployment + ClusterIP Service (포트 5432)

## 에이전트 행동 지침
- PostgreSQL 이미지를 변경할 때는 `pgvector/pgvector:pg16` 유지 (vector 확장 필수)
- `pg_uuidv7` 관련 SQL 절대 추가하지 마라 — 이 이미지에 없음
- PVC를 삭제하면 데이터가 전부 날아감. DB 비밀번호 변경 시 반드시 PVC까지 함께 재생성
