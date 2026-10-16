# 025. JWT 검증 적용 및 공개/인증 필요 엔드포인트 분리

## 상태

- [x] 확정됨 (Accepted)
- decision 013(임시 인증 스텁)의 `SecurityConfig` 부분을 대체한다.

## 배경

chapchu-auth 배포가 완료되어(`http://auth.chapchu.site:30900`) 실제 JWT 검증이 가능해졌다.
그 전까지 `SecurityConfig`는 모든 요청을 `permitAll()` 처리하는 임시 설정이었다(decision 013, issue #44).

## 결정

### 1. OAuth2 Resource Server로 JWT 검증

- `SecurityConfig`의 `permitAll()`을 `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))`로 교체했다.
- `JwtDecoder`는 별도로 선언하지 않는다. `application.yml`의
  `spring.security.oauth2.resourceserver.jwt.issuer-uri: ${AUTH_SERVER_URL}`를 통해
  OIDC 디스커버리(`/.well-known/openid-configuration` → `jwks_uri`)로 자동 구성된다.
  - chapchu-auth는 Spring Authorization Server이고 디스커버리/JWKS를 정상 제공한다(RSA 2048-bit).
  - 공개키를 레포에 하드코딩(`public-key-location`)하지 않는다. 키 롤링 시 코드 변경이 필요해지기 때문이다.
- 세션을 만들지 않는다(`SessionCreationPolicy.STATELESS`). 토큰 기반 API이므로 서버 세션이 불필요하다.
- `csrf().disable()`은 유지한다. 쿠키가 아닌 `Authorization` 헤더로 인증하므로 CSRF 대상이 아니다.

### 2. 공개/인증 필요 엔드포인트 분류

| 구분 | 대상 | 이유 |
|---|---|---|
| 공개 | `/actuator/health`, `/actuator/health/**` | k8s liveness/readiness probe가 토큰 없이 호출 |
| 공개 | `/docs/**` | REST Docs API 문서 (FE 개발자 참고용) |
| 공개 | `GET /places`, `GET /places/**`, `GET /weather` | 비로그인 사용자도 장소 탐색/날씨 조회는 가능해야 함 |
| 인증 필요 | 위를 제외한 **모든** 요청 | 개인화 데이터 조회 및 모든 쓰기 작업 |

- 공개 장소/날씨는 **GET에만** 허용한다. 같은 경로의 쓰기 요청은 인증을 요구한다.
- `GET /home`, `GET /users/me`, `GET /posts` 등은 개인 정보를 포함하거나 유저 컨텍스트가 필요하므로 공개하지 않는다.

## 남은 작업 (이 결정에 포함되지 않음)

- 각 Controller의 `TempAuthContext.TEMP_USER_ID`를 `@AuthenticationPrincipal Jwt`에서 추출한 실제 유저 UUID로
  교체하는 작업은 **범위가 커서 분리**한다(모든 도메인 컨트롤러/테스트가 영향). decision 013의 해당 항목은 아직 유효하다.
  → 별도 이슈로 진행하고, 완료 시 decision 013 전체를 폐기 처리한다.
- `AUTH_SERVER_URL`이 https로 전환되면(issue #37) 별도 코드 변경 없이 ConfigMap 갱신만으로 반영된다.

## 에이전트 행동 지침

- 새 엔드포인트를 추가할 때 기본값은 **인증 필요**다. 공개가 필요하면 이 문서의 표를 갱신하고
  `SecurityConfig`의 `PUBLIC_PATHS` / `PUBLIC_GET_PATHS`에 추가하라.
- 기존 컨트롤러 테스트는 `@AutoConfigureMockMvc(addFilters = false)`로 보안 필터를 우회하므로 영향받지 않는다.
  보안 규칙 자체를 검증하려면 `SecurityConfigTest`처럼 필터를 켜고 `JwtDecoder`를 목으로 주입하라.
