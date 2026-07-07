# 008. `ddl-auto: validate` 전환 후 BaseEntity PK 컬럼명 불일치

## 증상
Flyway 도입(decision 011) 후 `ddl-auto: validate`로 앱을 기동하면:
```
SchemaManagementException: Schema-validation: missing column [id] in table [pets]
```
(또는 breeds/users 등 다른 테이블에서 동일 패턴)

## 실패 원인
- `docs/schema/init.sql` / Flyway `V1__init_schema.sql`의 모든 PK 컬럼명은 `{테이블 단수형}_id` 규칙(`pet_id`, `breed_id`, `user_id` 등)을 따른다.
- `BaseEntity`의 `@Id` 필드는 컬럼명을 지정하지 않아 기본값인 `id`로 매핑된다.
- `ddl-auto: create`일 때는 Hibernate가 스키마를 직접 생성하므로 `id` 컬럼이 그냥 만들어져 문제가 드러나지 않았다. `validate`로 바뀌면서 실제 Flyway 스키마와 비교하다가 처음 발각됨.

## 해결책 (현재 적용)
`BaseEntity`를 상속하는 각 Entity 클래스에 `@AttributeOverride`로 실제 컬럼명을 명시한다:
```java
@Entity
@Table(name = "pets")
@AttributeOverride(name = "id", column = @Column(name = "pet_id"))
public class Pet extends BaseEntity { ... }
```

## 에이전트 행동 지침
- 새 Entity를 만들 때마다 `docs/schema/init.sql`(또는 해당 Flyway 마이그레이션)에서 그 테이블의 실제 PK 컬럼명을 확인하고, `BaseEntity` 상속 클래스에 반드시 `@AttributeOverride(name = "id", column = @Column(name = "{실제_컬럼명}"))`을 추가하라.
- `BaseEntity` 자체에 특정 컬럼명을 하드코딩하지 마라 — 테이블마다 PK 컬럼명이 다르다.
- 새 Entity를 추가하고 `ddl-auto: validate`로 로컬에서 최소 한 번은 기동해 스키마 검증 오류가 없는지 확인하라.
