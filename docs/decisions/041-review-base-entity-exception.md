# 041 — Review 엔티티: BaseEntity 미상속 예외

## 결정

`Review` 엔티티는 `BaseEntity`를 상속하지 않고 `@Id`, `createdAt`을 직접 선언한다.

## 이유

`reviews` 테이블의 PK 컬럼명은 `review_id`(V1__init_schema.sql:280)이나,
`BaseEntity`의 `@Id` 필드는 Hibernate 기본 네이밍(`id` 컬럼)을 사용한다.
`BaseEntity`를 그대로 상속하면 `id` → `review_id` 컬럼 매핑이 깨져 런타임 오류가 발생한다.

컬럼명 재정의(`@AttributeOverride`)로 해결하면 BaseEntity가 사실상 빈 껍데기가 되어
상속의 이점이 없다.

## 허용 조건

아래 두 규칙은 Review 엔티티에서도 동일하게 준수한다:

- `@GeneratedValue` 사용 금지 — `UuidCreator.getTimeOrderedEpoch()`로 UUID v7 직접 생성
- PK 타입: `UUID`

## 참고

- decisions/036: 코드값 테이블(`breeds` 등) BaseEntity 미상속 예외 (INT PK 이유)
- V1__init_schema.sql:280: `reviews.review_id` PK 정의
