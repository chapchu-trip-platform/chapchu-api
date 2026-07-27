# 016. 장소 데이터 출처: 한국관광공사 TourAPI (실시간 호출 + DB 캐시)

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 장소 데이터는 **한국관광공사 TourAPI 5.0** (공공데이터포털)을 서버에서 직접 호출한다.
- 호출 결과를 `places` / `place_pet_policies` 테이블에 upsert(캐시)한다.
- `external_place_id` = 한국관광공사 **contentId** (카카오 장소 ID 아님).
- 클라이언트가 장소 데이터를 직접 서버에 전송하는 `POST /places` 방식은 사용하지 않는다.

## 흐름
```
사용자 (위치 + 요청)
    ↓
우리 서버
    ↓ (DB에 없거나 만료된 경우)
한국관광공사 TourAPI
  - locationBasedList: 위치 기반 반려동물 동반 가능 장소 목록
  - detailPetTour: 반려동물 정책 상세 (contentId별)
    ↓
places / place_pet_policies upsert
    ↓
RAG (place_embeddings 벡터 검색) → 코스 추천
```

## 사용 API
- `locationBasedList`: 위치(mapX, mapY, radius) 기반 관광지 목록, `petTour=Y` 필터
- `detailPetTour`: contentId별 반려동물 동반 상세 정보 (입장 규정, 목줄 등)

## 이유
- DB 설계(`external_place_id VARCHAR PK`)가 이미 외부 API ID 캐시 방식으로 설계되어 있음.
- 클라이언트가 장소 데이터를 직접 전송하면 신뢰할 수 없는 데이터가 DB에 적재될 수 있음.
- 한국관광공사 API가 반려동물 동반 가능 여부를 공식 제공하므로 별도 크롤링 불필요.

## 에이전트 행동 지침
- `POST /places` (클라이언트 직접 전송) 엔드포인트를 만들지 마라.
- 장소 조회/코스 추천 흐름에서 한국관광공사 API 호출은 서버 내부 서비스(`TourApiClient`)가 담당한다.
- TourAPI 키는 환경변수 `TOUR_API_KEY`로 관리한다. 코드에 하드코딩하지 마라.
- API 호출 결과는 항상 우리 DB에 upsert한 뒤 반환한다 (캐시 우선).
