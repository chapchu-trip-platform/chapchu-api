# 015. pg_uuidv7 확장 — pgvector/pgvector:pg16 이미지에 없음

## 증상
```
Caused by: org.postgresql.util.PSQLException: 
ERROR: extension "pg_uuidv7" is not available
```
Flyway V1 마이그레이션 실패, chapchu-api CrashLoopBackOff.

## 원인
V1__init_schema.sql 초기 버전에 `CREATE EXTENSION IF NOT EXISTS pg_uuidv7` 와
`uuid_generate_v7()` 기본값이 있었으나 `pgvector/pgvector:pg16` 이미지에는 해당 확장이 포함되어 있지 않다.

## 해결
V1__init_schema.sql 에서 `pg_uuidv7` 확장 및 `uuid_generate_v7()` 를 제거하고
PostgreSQL 내장 `gen_random_uuid()` (UUID v4) 로 교체.

```sql
-- 제거
CREATE EXTENSION IF NOT EXISTS pg_uuidv7;
-- uuid_generate_v7() → gen_random_uuid()
```

UUID v7이 필요한 경우 애플리케이션 레벨에서 `uuid-creator:5.3.7` 라이브러리 사용 (decisions/005 참조).

## 에이전트 행동 지침
- `pg_uuidv7` 또는 `uuid_generate_v7` 를 SQL에 절대 추가하지 마라.
- DB 기본값은 `gen_random_uuid()`, 앱 코드에서 UUID v7이 필요하면 `UuidCreator.getTimeOrderedEpoch()` 사용.
