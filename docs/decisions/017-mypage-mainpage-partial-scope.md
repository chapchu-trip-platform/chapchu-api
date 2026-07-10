# 017. 마이페이지 요약 / 메인 페이지 조회 — 1차 범위 (부분 구현)

## 상태
- [x] 확정됨 (Accepted, 확장 예정)

## 결정
- `GET /users/me/mypage`: 닉네임, 이메일, 반려견 수만 반환한다.
- `GET /main`: 닉네임, 반려견 이름 목록만 반환한다.
- `docs/decisions/006`에서 담당자 "미정"이던 두 엔드포인트를 이번에 구현하되, 날씨/추천 여행 코스/스탬프/게시글 등 아직 구현되지 않은 도메인(이정범 담당 weather, trip, album, 커뮤니티) 데이터는 포함하지 않는다.
- 작성한 리뷰 수(`reviewCount`)도 이번 범위에서 제외한다. review 도메인(PR #19)이 아직 `dev`에 merge되지 않은 상태에서 브랜치를 분기했기 때문이며, merge 후 후속 PR로 추가한다.

## 이유
- 두 엔드포인트는 여러 도메인을 조합하는 성격이라, 의존 도메인이 다 구현되기 전까지는 부분 구현으로 시작하고 점진적으로 필드를 확장하는 편이 각 PR을 독립적으로 리뷰 가능하게 유지하는 데 유리하다.
- review 도메인과 별개 PR로 분리해 두 PR이 서로의 diff를 오염시키지 않도록 했다.

## 에이전트 행동 지침
- review 도메인(PR #19)이 `dev`에 merge되면 `MyPageService.getSummary`에 `reviewCount` 필드를 추가하는 후속 PR을 만들어라. `module-user/build.gradle`에 `implementation project(':module-review')`를 추가하고 `ReviewRepository.countByUserId`를 사용하면 된다.
- weather(날씨), trip(여행 코스 추천) 도메인이 구현되면 `MainPageService`에 필드를 추가로 조합하라. 기존 필드(`nickname`, `petNames`)의 이름/타입은 바꾸지 말고 확장하라 (하위 호환).
- stamp, album, 커뮤니티(게시글) 도메인이 구현되면 `MyPageSummary`에 관련 카운트 필드를 추가하라.
