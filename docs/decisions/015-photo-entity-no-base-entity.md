# 015. Photo 엔티티는 BaseEntity를 상속하지 않는다

## 상태
- [x] 확정됨 (Accepted)

## 결정
- `com.pettrip.photo.model.Photo`는 `BaseEntity`를 상속하지 않고, PK(`photo_id`)와 `createdAt`만 직접 선언한다.

## 이유
- `photos` 테이블(`V1__init_schema.sql`)에는 `updated_at` 컬럼이 없다 (`created_at`만 존재).
- `BaseEntity`는 `@LastModifiedDate`로 `updated_at` 컬럼 매핑을 강제하는데, JPA는 상속받은 매핑 속성을 서브클래스에서 제외할 방법이 없다.
- `updated_at`이 없는 실제 스키마에 맞추기 위해 `BaseEntity`를 상속하지 않고 별도로 `id`/`createdAt`만 선언했다.

## 에이전트 행동 지침
- 새 Entity를 만들 때 해당 테이블에 `updated_at` 컬럼이 없다면 `BaseEntity`를 상속하지 말고 `Photo`와 동일한 패턴(직접 `@Id` + `@CreatedDate`만 선언)을 따르라.
- `photos` 테이블에 `updated_at`을 추가하는 방식으로 우회하지 마라 — 스키마는 실제 요구사항(수정 이력 불필요)을 반영한 것이다.
