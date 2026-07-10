# 017. 장소 위시리스트 테이블 신규 도입

## 상태
- [x] 확정됨 (Accepted)

## 배경
- 신규 API 명세에 장소 위시리스트 기능이 추가됨: `GET /users/me/wishlist`, `DELETE /users/me/wishlist/{placeId}`.
- 기존 스키마(`V1__init_schema.sql`)에는 위시리스트를 저장할 테이블이 전혀 없었다.

## 결정
- `place_wishlists` 테이블을 `V4__create_place_wishlists.sql`로 추가한다.
- PK는 `(user_id, place_id)` 복합키로 하고, `post_bookmarks`와 동일한 `@IdClass` 패턴을 따른다.
- `place_id`는 `places.external_place_id`(VARCHAR(255))를 참조한다. PK 타입이 UUID가 아니라 VARCHAR임에 유의.
- 테이블에 `updated_at`이 없으므로 `Wishlist` 엔티티는 `BaseEntity`를 상속하지 않고 `@Id` + `@CreatedDate`만 직접 선언한다 (ADR 015 준수).
- 엔티티는 `module-user`의 `com.pettrip.wishlist` 도메인에 위치시킨다. `places` 테이블을 JPA `@ManyToOne`으로 참조하지 않고 `place_id`를 plain `String` 컬럼으로만 보관한다 (모듈 간 느슨한 결합).

## 미해결 사항 (이정범 확인 필요)
- 신규 명세에 위시리스트 **추가(POST)** 엔드포인트가 없다. GET/DELETE만으로는 데이터를 넣을 방법이 없어 기능이 완성되지 않는다. 별도 이슈로 등록함.

## 에이전트 행동 지침
- 위시리스트 추가 엔드포인트가 확정되면 이 도메인에 `POST` 핸들러와 `existsByUserIdAndPlaceId` 중복 방지 로직을 추가하라.
