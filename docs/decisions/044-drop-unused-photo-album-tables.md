# 044 — albums, album_photos, visit_verifications 테이블 제거

## 결정

`albums`, `album_photos`, `visit_verifications` 세 테이블을 DROP한다(V30). 빈 `module-album` 모듈도 제거한다.

## 배경

V1 초기 스키마에서 앨범 도메인(`albums`/`album_photos`)과 방문인증(`visit_verifications`)을 미리 설계했으나, 실제 구현이 다른 방향으로 확정되면서 세 테이블 모두 쓰이지 않게 됐다.

- **앨범**: "내 앨범"은 `GET /users/me/album`이 `review_photos`에서 내가 쓴 리뷰의 사진을 펼쳐 보여주는 방식으로 동작한다(파생). 사용자가 정의하는 앨범 엔티티(`albums`)는 도입되지 않았다.
- **방문인증**: 장소 인증(좌표·상태 판정)은 FE가 전담하기로 확정됐다. 백엔드는 사진(`photos`)만 다루고 인증 레코드는 저장하지 않는다.

## 제거 근거

| 항목 | 상태 |
|---|---|
| Java 코드 참조 | 0건 |
| Entity / Repository / Service | 없음 |
| 컨트롤러 / 엔드포인트 | 없음 |
| 다른 테이블의 FK 참조 | 없음 (세 테이블 모두 leaf) |
| `ddl-auto: validate` 기동 | 매핑 엔티티 없어 DROP 후에도 안전 |
| API 요청/응답 영향 | 없음 (`GET /users/me/album`은 review_photos 기반이라 불변) |

## 앞으로의 앨범/사진 모델 (방향)

- 앨범 = `photos.course_place_id → course_places → travel_courses` 파생. 코스 단위로 여행 사진을 모으고, 펫 사망 시 `pets.is_die` 플래그로 펫 단위 열람으로 전환(테이블 분리 없음).
- 사진 공개여부 = **연결로 판단**: `review_photos`(또는 `post_photos`)에 연결되면 전체공개, 아니면 개인. 별도 컬럼 두지 않는다.

## 관련

- 이슈: #267
- 선례: docs/decisions/042 (place_embeddings·course_embeddings 제거)
- 후속(별개): 앨범 course_place 기반 재작성, 코스 장소 상세에 타인 리뷰 사진 노출
