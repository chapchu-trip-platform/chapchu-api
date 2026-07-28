---
name: 016-pets-age-smallint-hibernate-validation
description: V1 schema pets.age SMALLINT vs Pet entity Integer — Hibernate ddl-auto validate 실패
---

# 016. pets.age SMALLINT ↔ Integer 타입 불일치 — Hibernate 검증 실패

## 증상
```
SchemaManagementException: Schema-validation: wrong column type encountered
in column [age] in table [pets];
found [int2 (Types#SMALLINT)], but expecting [integer (Types#INTEGER)]
```
Flyway 마이그레이션(V2/V3)은 성공했으나 Hibernate `ddl-auto: validate` 단계에서 앱이 CrashLoopBackOff.

## 원인
- `V1__init_schema.sql` 의 `pets.age` 컬럼이 `SMALLINT`(`int2`)로 정의됨.
- `com.pettrip.pet.model.Pet`의 `age` 필드는 `Integer` → Hibernate가 PostgreSQL `integer`(`int4`)로 매핑.
- `SMALLINT` ≠ `INTEGER` → 검증 실패.

## V1 수정 불가 이유
`V1`은 production DB의 `flyway_schema_history`에 `success = true`로 기록됨.
수정 시 Flyway 체크섬 충돌 → 기존 서버 기동 불가.

## 해결 (V4 마이그레이션)
`V4__fix_pets_age_type.sql`:
```sql
ALTER TABLE pets ALTER COLUMN age TYPE INTEGER;
```
`docs/schema/init.sql`의 `age SMALLINT` → `age INTEGER`도 함께 갱신.

## 에이전트 행동 지침
- `V1`의 다른 `SMALLINT` 컬럼(`places.rating`, `course_places.visit_order` 등)도
  대응 Entity 구현 시 동일하게 `Integer` 매핑이면 같은 오류가 발생한다.
  해당 도메인 구현 전에 `V{n}__fix_{table}_{column}_type.sql` 마이그레이션을 먼저 추가하라.
- `ddl-auto: validate`는 절대 `update`로 바꾸지 마라 — Flyway가 DDL 담당, JPA는 검증만.
