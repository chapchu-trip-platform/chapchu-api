# 043 - 코스 큐레이션 공식 (intermediateStopCount 제거 → 거리·체력·날씨 기반 스탑 수 + AI 큐레이션)

## 상태
확정 (2026-09-10). PR3(코스 재설계)에서 구현.

## 배경 / 문제
기존 `POST /courses`는 출발→도착 직선을 `intermediateStopCount + 2`개 구역(bounding box)으로 쪼개 구역당 1곳씩 지리순으로 뽑았다. 문제:
- 사용자가 **중간 개수를 직접 지정**해야 함(반려인은 "몇 개"를 모른다).
- 얇은 bbox가 라인 밖 장소를 버려 스탑이 적게 나옴(서울/대구 2곳, 부산 1곳).
- **도착지에 반려견이 못 가는 곳**(예: 강남역 CGV)이 찍힐 수 있음.
- 반려동물 **취향**(예: 수영)·**체력**(나이/크기)이 스탑 수/선택에 반영 안 됨.

## 결정

### 1. 흐름 (도착지 선택형)
1. 사용자가 대략 목적지 좌표 입력 → `POST /recommended-places {도착지 좌표, limit:5}`로 **반려견 장소 후보 5개**(취향·날씨 반영 RAG 랭킹, PR2 완료).
2. 사용자가 하나 고름 → **그 후보 장소 정보를 통째로**(recommend 응답 항목 그대로) 코스 생성 요청에 담아 보냄.
3. `POST /courses`는 `intermediateStopCount` 없이, **받은 도착지 정보로 Place를 upsert**한 뒤 그 장소를 **고정 도착 스탑**으로 두고 중간 스탑을 AI가 큐레이션.

> **왜 도착지를 통째로 보내나**: FE가 이미 recommend에서 받은 장소 데이터를 다시 보내면, 서버가 TourAPI 재조회/DB 재동기화에 의존하지 않고 그 데이터로 바로 Place를 저장·사용한다. `recommendPlaces`가 readOnly라 도착지 Place가 DB에 없을 수 있는 함정을 원천 제거.

### 2. 코스 구성
```
출발지(좌표, 저장 스탑 아님) → [큐레이션 스탑 0~N개] → 고정 도착지(사용자가 고른 반려견 장소)
```
- 출발지는 "출발 지점 좌표"일 뿐 CoursePlace로 저장하지 않음.
- 도착지는 `endExternalPlaceId`로 고정(DB Place 조회, 없으면 400). 항상 마지막 스탑.
- 저장 스탑 수 = 큐레이션(0~N) + 도착지 1 = 최대 N+1곳.

### 3. N(중간 스탑 상한) 공식 = 거리 × 체력 × 날씨
```
distanceBase(직선거리 d):  d < 4km → 1,  4 ≤ d < 9km → 2,  d ≥ 9km → 3
petAdj:      (size == SMALL) 또는 (age ≥ 8)  → -1,  그 외 0
weatherAdj:  비/눈/폭염(기온 ≥ 30)          → -1,  그 외 0
N = clamp(distanceBase + petAdj + weatherAdj, 1, 3)
```
- N은 **상한(cap)**. AI는 후보 풀에서 **0~N개**를 취향·리듬·날씨·동선을 보고 큐레이션(적합한 게 없거나 풀이 비면 0개도 가능).
- "공식이 상한을 정하고, 그 안에서 AI가 알잘딱" 하이브리드.

### 4. 후보 풀 수집 (무구역, bbox 폐기)
- 기존 구역 루프 / `searchInZone` / `filterByZone`(bbox) **전면 제거**.
- 출발지 + 출발→도착 직선의 표본 지점(예: 1/3·2/3 지점)을 `searchAroundPoint`(#241 헬퍼)로 반경 검색 → dedup(externalPlaceId) → 도착지 자신 제외 → 풀 상한(예: 30) 컷.
- 반려동물 크기 필터 + 취향 RAG 랭킹(PR2 buildRagQuery) 적용 후 풀 구성.

### 5. AI 큐레이터
- 입력: 후보 풀(각 장소 category·indoorOutdoor·petPolicy 요약·좌표), 반려동물(크기·나이·취향), 날씨, 출발 좌표, 고정 도착지.
- 지시: 출발→도착 방향으로 자연스럽게, 취향(수영→물) 가산, 날씨 나쁘면 실내 우선, 카테고리 다양성(산책·식사·놀거리), **N개 이하** 선택 + 스탑별 이유.
- (참고) 후보 풀은 이미 반려동물 크기 하드필터를 통과하므로 크기 위반은 없음. petPolicy(목줄·주의사항)를 프롬프트에 직접 넣는 건 후속(PlaceInfo 필드 추가 churn 회피). 이번 PR은 category·indoorOutdoor·취향·날씨로 큐레이션.
- 출력 파싱 실패 시 fallback: 풀 상위 N개(이유 없음).
- 도착지 이유는 기본값("사용자가 선택한 도착지").

## 검토한 엣지케이스
1. **초단거리(d<1km)**: base 1, 보정으로 내려가도 clamp로 N≥1. AI가 적합한 게 없으면 0개 → 코스=도착지 1곳. 억지 스탑 강요 안 됨. OK.
2. **희박 지역(부산)**: N=3이어도 풀이 1개면 AI가 ≤1개 선택. 자연히 처리.
3. **빈 풀**: 큐레이션 0개 → 코스=도착지 1곳. **404 안 남**(도착지는 고정·유효). 기존 404 문제 해소.
4. **도착지 미동기화 (해소됨)**: FE가 도착지 정보를 통째로 보내므로, 서버가 그 데이터로 **Place(+PlacePetPolicy) upsert** 후 사용 → DB 조회 실패/400 위험 없음. TourAPI 재조회 불필요.
   → **구현 주의**: PlacePetPolicy는 `@OneToOne @MapsId Place`라, 반드시 **upsert된(관리 상태의) Place 인스턴스**로 정책을 저장해야 함(#202 DuplicateKey 교훈 재적용). Place 저장 → 관리 인스턴스 획득 → 정책 저장 순서.
5. **되돌아감(우회)**: 프롬프트가 출발→도착 방향 명시 + 풀을 회랑 표본으로 모아 라인에서 먼 후보를 애초에 줄임.
6. **null 값**: 날씨/나이/크기 null이면 해당 보정 스킵(graceful).
7. **이중 감점(소형 노령 + 악천후)**: base2 −1 −1 = 0 → clamp 1. 상한은 1로 유지하되 AI가 더 적게(0) 고를 수 있음 — "지친 아이 무리시키지 않기"와 "최소한의 코스" 절충.
8. **초장거리 도심 밖(d=50km)**: cap 3 유지. 하루 코스 취지에 맞음(로드트립 아님).

## 확정한 노브 (기본값)
- **N 하한(cap floor) = 1**: 상한이 최소 1이라 AI가 최소한 한 곳을 고려. 실제 0곳은 "적합/데이터 없음"일 때만.
- **거리 구간 = 4km / 9km**: 한국 도심 하루 이동 기준.
- **노령 기준 = 나이 ≥ 8**: 견종별 차이는 단순화(향후 견종 가중 여지).
- **폭염 기준 = 기온 ≥ 30℃**, 악천후 = 비/눈 계열.

## 대안 (기각)
- **AI가 N까지 전적으로 결정(공식 없음)**: 예측 불가·비용 편차 큼. 상한 공식으로 가드.
- **bbox 유지 + 패딩**: 얇은 띠 문제 근본 해결 안 됨(#241 후속 검토서 이미 지적). 폐기.
- **거리 비례 연속식(d/4 반올림)**: 구간식이 설명·튜닝 쉬움 → 구간식 채택.

## 영향
- `CreateCourseRequest`: `intermediateStopCount` 제거. 도착지를 개별 필드(endLocation/endLat/endLng) 대신 **`destination` 객체**(recommend 응답 항목 형태: externalPlaceId·placeName·placeImageUrl·lat·lng·address·categoryLabel·indoorOutdoorType·allowedPetSize·leashRequired·carrierRequired·placeCaution)로 받음.
- 도착지 Place/PlacePetPolicy **upsert** 로직 추가(PlaceService 또는 CourseService). 관리 인스턴스 순서 주의(#202).
- `CourseService.createCourse`: bbox 로직 제거 → 풀 수집 + N 공식 + 큐레이션.
- `RouteOptimizationService`: `selectAndOrder`(n 기반) → `curateCourse`(풀 기반, 이유 반환).
- `CoursePlace`/`CourseResponse`: `reason` 추가(V29).
- CLAUDE.md 코스 불변규칙(구역/개수 관련) 갱신 필요.
