# 039. 커뮤니티 목업 데이터를 Flyway 마이그레이션으로 심는다

## 배경

`GET /posts`, `GET /posts?sort=popular`가 항상 빈 배열을 돌려줘 프론트에서 목록·정렬·상세를
붙여볼 방법이 없었다. 글을 하나 만들려면 `POST /posts`에 `petId`, `photoId`, `courseId`가
전부 필요한데, 사진 업로드(S3 버킷이 아직 placeholder, #101)와 코스 생성(#126, 미완)이
막혀 있어 손으로도 만들 수 없다.

`posts`는 세 FK가 모두 NOT NULL이라 글 한 줄을 넣으려면 아래가 전부 있어야 한다.

```
users ─┬─ pets
       └─ start_course → travel_courses → course_places → photos
                                 ↑
                              places
```

`places`는 TourAPI 응답을 동기화해 채우는 테이블이라(030) 비어 있을 수 있다.

## 결정

`V11__seed_mock_community_posts.sql`에서 위 체인을 전부 심는다.
작성자 5명, 반려견 5마리, 장소 6곳, 코스 5개, 사진 6장, 게시글 12개.

### 고정 UUID

모든 행의 PK를 `00000000-0000-4000-8000-0000000000..` 형태로 고정한다.
진짜 데이터가 쌓인 뒤에도 이 접두사만으로 목업을 정확히 골라낼 수 있다.
지울 때는 INSERT 역순(posts → photos → course_places → travel_courses → start_course
→ pets → users → places)으로 DELETE 하면 된다.

### 추천 수를 작성 시각과 어긋나게 배치

최신순과 추천순 결과가 같으면 정렬이 도는지 확인할 수 없다. 가장 최근 글에 추천 214를,
2주 전 글에 8을 주는 식으로 두 순서가 눈에 띄게 달라지게 했다.
추천 23으로 동점인 글 두 개를 두어 동점일 때 최신 글이 앞에 오는 2차 정렬
(`findAllByOrderByRecommendationCountDescCreatedAtDesc`)도 확인할 수 있다.

### `post_recommendations`에는 행을 넣지 않는다

추천 수는 `posts.recommendation_count` 컬럼으로만 관리된다(`PostService.recommend`가
컬럼을 증감시키고, 목록 조회는 컬럼만 읽는다). 목업 유저 5명으로는 214를 만들 수 없고,
행 수와 컬럼 값을 맞추려면 유저 214명을 심어야 한다.
실제 유저가 추천/취소를 누르면 이 값에서 그대로 오르내리므로 동작에는 영향이 없다.

### `photo_url`은 비운다

S3 버킷이 placeholder라 진짜 URL이 없다(#101). 가짜 URL을 넣으면 프론트에서
깨진 이미지로 보인다. 컬럼이 nullable이므로 NULL로 둔다.

### 목업 장소는 검색 결과에 섞이지 않는다

`PlaceService.searchNearby`는 TourAPI 응답만 돌려주고 DB를 조회하지 않는다.
`MOCK-PLACE-*`는 `GET /places/{externalPlaceId}`로 직접 지정해야만 나온다.

## 대안

**dev 프로파일 전용 시더**(`@Profile("dev")` CommandLineRunner)를 검토했다.
운영 DB를 더럽히지 않는다는 장점이 있지만, 지금 프론트가 붙는 곳이 배포 환경 하나뿐이라
그 환경에 데이터가 없으면 목적을 달성하지 못한다. 프로파일이 분리되면 그때 옮긴다.

## 에이전트 행동 지침

- 목업 행을 하나씩 UPDATE 하지 마라. 내용을 바꿔야 하면 새 마이그레이션에서
  `00000000-0000-4000-8000-%` 전체를 지우고 다시 심어라.
- 진짜 데이터가 들어오기 시작하면 이 마이그레이션을 되돌리는 것이 아니라
  **삭제 마이그레이션을 새로 추가**하라. Flyway는 additive-only다.
- `MOCK-PLACE-*`와 `mock-google-*`, `@mock.chapchu.site`는 목업 표식이다.
  실제 유저 통계·목록을 다루는 기능을 만들 때 이 접두사를 제외할지 판단하라.
