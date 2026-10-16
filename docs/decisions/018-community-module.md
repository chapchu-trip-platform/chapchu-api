# 018. 커뮤니티(게시판) 도메인 — module-community 신설

## 상태
- [x] 확정됨 (Accepted)

## 결정
- `posts`/`comments`/`post_recommendations`/`post_bookmarks`/`post_reports` 테이블을 다루는 새 Gradle 모듈 `module-community`를 만든다.
- 패키지는 `com.pettrip.post.*`, `com.pettrip.comment.*`로 나눈다 (모듈명은 `community`, 도메인 패키지는 기능 단위로 세분화 — `module-user`가 `pet`/`photo`/`user` 패키지를 함께 담는 것과 동일한 방식).
- `Post` 엔티티는 `pet_id`, `photo_id`, `course_id`를 다른 모듈의 엔티티 참조 없이 순수 UUID 컬럼으로만 저장한다 (기존 `Review.petId` 패턴과 동일). `module-community`가 `module-user`/`module-trip`을 컴파일 의존하지 않도록 하기 위함이다.
- `posts.course_id`는 DB 제약상 `NOT NULL`이지만, 여행 코스 생성(`POST /courses`, 이정범 담당)이 아직 구현되지 않았다. 따라서 실제 서비스 환경에서 게시글 작성은 여행 코스 도메인이 완성되기 전까지 정상 동작하지 않는다. 코드/테스트는 지금 완성하되, 이 제약은 통합 전까지 알려진 한계로 남긴다.

## 이유
- `docs/decisions/006`에서 담당자 "미정"으로 표시된 커뮤니티 도메인 14개 엔드포인트를 구현하되, 기존 도메인 모듈(review, place, trip, album)과 섞이지 않도록 독립 모듈로 분리했다.

## 에이전트 행동 지침
- 여행 코스 생성(POST /courses)이 구현되면 이 문서의 "알려진 한계" 항목을 제거하라.
- `comments`, `post_recommendations`, `post_bookmarks`, `post_reports`도 이 모듈(`module-community`)에 추가하라. 새 모듈을 만들지 마라.
