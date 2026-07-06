# 011. 임시 인증 스텁 (실제 OAuth2 Resource Server 붙기 전)

## 상태
- [x] 확정됨 (Accepted, 임시)

## 결정
- `auth`/`user` 도메인의 실제 JWT 인증(decision 008, chapchu-auth 연동)이 구현되기 전까지:
  - `app/src/main/java/com/pettrip/config/SecurityConfig.java`에 모든 요청을 `permitAll()` 처리하는 임시 `SecurityFilterChain`을 둔다.
  - 인증이 필요한 Controller는 `@AuthenticationPrincipal` 대신 고정 상수 `TEMP_USER_ID`(예: `PetController.TEMP_USER_ID`)를 현재 유저로 사용한다.

## 이유
- pet 등 유저 소유 리소스 도메인을 auth 도메인보다 먼저 구현하게 되어, 실제 인증 없이도 소유권 로직/TDD/REST Docs 패턴을 검증할 수 있어야 했음.

## 에이전트 행동 지침
- auth 도메인(JWT 검증)이 구현되면:
  1. `SecurityConfig`의 `permitAll()`을 실제 OAuth2 Resource Server 설정으로 교체하라.
  2. 각 Controller의 `TEMP_USER_ID` 상수를 `@AuthenticationPrincipal Jwt jwt`에서 추출한 실제 유저 UUID로 교체하라.
  3. 이 문서의 상태를 "폐기됨(Superseded)"으로 갱신하고, 실제 인증 결정을 새 번호로 별도 기록하라.
- 그 전까지는 `permitAll()` 설정과 `TEMP_USER_ID` 패턴을 다른 도메인(예: photo, user)에도 동일하게 적용해 일관성을 유지하라.
