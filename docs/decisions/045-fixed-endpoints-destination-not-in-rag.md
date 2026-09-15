# 045 — 코스 출발/도착 고정 + 도착지 RAG 미축적

## 결정

코스의 **출발지·도착지는 고정 양 끝점**이고 시스템은 **중간 스탑만** 추천한다.
도착지는 **사용자가 임의로 고른 지점**(우리 펫장소 DB·`/recommended-places` 무관)이며, **`places`/RAG 코퍼스에 축적하지 않는다.**
단 도착지는 실제 방문지라 **리뷰·사진·앨범 대상**이어야 하므로, `places`에 넣지 않고 **`course_places` 스탑으로만 비정규화 저장**한다.

## 배경

기존(decisions/043)에는 도착지를 `/recommended-places` 후보(반려견 장소)에서 고르게 하고 `PlaceService.upsertPlace`로 `places`(+`place_pet_policies`)에 영구 저장했다. 이 도착지는 이후 `searchNearby` 후보 풀·`PlaceRagService` 랭킹에 다시 등장해 **RAG/추천 코퍼스를 오염**시켰다. 도착지는 "여행의 끝점"일 뿐 추천 대상이 아니므로 코퍼스에서 분리한다.

## 방식

| 항목 | 처리 |
|---|---|
| 도착지 upsert | **제거** — `places`/`place_pet_policies`에 저장 안 함 |
| 도착지 저장 위치 | `course_places` 스탑 1행. `external_place_id=NULL` + `place_name·latitude·longitude` 비정규화(V33) |
| 리뷰·사진·앨범 | `course_place_id` 기준이라 **그대로 동작**(도착지도 스탑) |
| 도착지 반려견 가능여부 | 요청의 `allowedPetSize` vs `pet.size`로 **플래그만 계산**해 `course_places.pet_allowed`에 저장(V33). 막지 않고 표시만. places/RAG와 무관 |
| 중간 스탑 | 변경 없음 — `searchNearby`(midpoint 반경) 후보를 `places`에 동기화 + 크기필터 + 취향 RAG 랭킹 + AI 큐레이션 |
| 출발지 | 변경 없음 — `travel_courses.start_*` 좌표만(place 아님) |

## 요청/응답 영향

- `POST /courses` `Destination`: `externalPlaceId` 필수 해제. 저장은 `placeName·latitude·longitude`만. `allowedPetSize`는 저장하지 않되 **반려견 가능여부 플래그 계산에만** 사용(나머지 정책 필드는 무시).
- `GET /courses/{id}`: 도착지 스탑의 `externalPlaceId`가 **null**일 수 있음(이름·좌표는 `course_places` 비정규화 필드에서 채움). 스탑에 `petAllowed`(true/false/모르면 null) 포함 — **막지 않고 표시만**(반려견 불가 장소도 코스는 생성됨).
- 앨범 응답: 도착지 사진의 `externalPlaceId`도 null 가능.

## 대안 (기각)

- **도착지를 `places`에 넣되 "추천 제외 플래그"로 필터**: 여전히 코퍼스에 축적됨 → "안 쌓기" 요구에 어긋나 기각.
- **도착지를 `travel_courses` 끝점 좌표로만(스탑 아님)**: 제일 단순하나 도착지에서 찍은 사진·리뷰가 course_place에 안 잡혀 앨범/리뷰에서 누락 → 기각.

## 관련

- 이슈: #275
- 마이그레이션: V33
- 이전: docs/decisions/043(코스 큐레이션), 앨범 재설계(#272)
