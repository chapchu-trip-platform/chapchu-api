# 016. chapchu-auth JWT 계약 (Resource Server 전환 시 참고)

## 상태
- [x] 확정됨 (Accepted)

## 결정
- chapchu-auth(별도 레포, `chapchu-trip-platform/chapchu-auth`)가 발급하는 JWT의 claim 계약:
  - `sub`: chapchu-api `users.user_id` (UUID v7 문자열). Google 식별자가 아니다.
  - `email`: `users.email`
  - `role`: `users.role` (`USER` | `ADMIN`)
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`(`AUTH_SERVER_URL`)는 chapchu-auth의 issuer URL을 가리키며, chapchu-auth와 정확히 동일한 값이어야 한다 (chapchu-auth 쪽 환경변수도 동일하게 `AUTH_SERVER_URL`).
- chapchu-auth는 chapchu-api와 동일한 Postgres의 `users` 테이블을 직접 공유 매핑한다 (chapchu-auth의 `docs/decisions/002-shared-users-table.md` 참고). chapchu-api를 호출해 사용자를 조회/생성하지 않는다.

## 이유
- decision 008에서 정한 "chapchu-auth가 JWT를 발급하고 chapchu-api는 서명 검증만 한다"는 원칙을 실제 claim 단위로 구체화해 두어야, 013(TempAuthContext) 제거 시 바로 적용할 수 있다.

## 에이전트 행동 지침
- `TempAuthContext.TEMP_USER_ID`를 실제 인증으로 교체할 때:
  1. `SecurityConfig`의 `permitAll()`을 제거하고 `spring.security.oauth2.resourceserver.jwt.issuer-uri: ${AUTH_SERVER_URL}` 설정으로 Resource Server를 활성화하라 (이미 `application.yml`에 설정돼 있음, `AUTH_SERVER_URL`만 실제 값으로 채우면 됨).
  2. 각 Controller에서 `@AuthenticationPrincipal Jwt jwt`를 받아 `UUID.fromString(jwt.getSubject())`로 현재 유저 UUID를 얻어라. `jwt.getClaimAsString("role")`로 권한을 확인할 수 있다.
  3. `POST /auth/google` 같은 토큰 발급 엔드포인트를 chapchu-api에 추가하지 마라 (decision 008). 로그인은 전적으로 chapchu-auth의 `/oauth2/authorize` 흐름을 통한다.
