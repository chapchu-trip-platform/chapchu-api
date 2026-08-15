# 033. BFF 패턴 — chapchu-api가 OAuth2 Client로 인가 흐름 전담

## 배경

초기 설계에서는 FE가 chapchu-auth의 PKCE 공개 클라이언트로서 OAuth 흐름을 직접 처리했다.
FE가 authorization code를 받아 chapchu-auth에 직접 토큰을 교환하고, registration_token / access_token을
클라이언트에서 관리했다.

문제는 두 가지였다.

첫째, access_token이 브라우저 메모리나 localStorage에 평문으로 존재해야 했다.
PKCE로 code 탈취는 막을 수 있지만, 토큰 자체가 JS 레이어에 노출된다.

둘째, FE가 chapchu-auth 내부 엔드포인트(`/oauth2/authorize`, `/oauth2/token`)를 직접 알아야 했다.
auth 서버 구조가 바뀌면 FE도 같이 바꿔야 한다.

## 결정

chapchu-api가 OAuth2 Confidential Client로서 인가 흐름 전체를 담당한다.

```
FE → GET /auth/login
chapchu-api → Google 로그인 리다이렉트
Google → chapchu-api /auth/callback
chapchu-api → chapchu-auth 토큰 교환
chapchu-api → refresh_token HttpOnly 쿠키 Set
chapchu-api → FE로 #access_token fragment 리다이렉트
```

FE는 `/auth/login` 하나만 알면 된다. PKCE·code 교환·refresh_token 관리는 chapchu-api 전담.

### 구현 포인트

- `state`는 stateless를 유지하기 위해 쿠키(`oauth2_state`)에 직렬화해 저장한다.
- `refresh_token`은 HttpOnly 쿠키(`/auth/refresh` 경로 한정)로 저장해 JS에서 접근 불가.
- `access_token`은 URL fragment(`#access_token=xxx`)로 전달한다. fragment는 서버 로그에 남지 않고
  브라우저 히스토리에도 기록되지 않는다. FE가 hash에서 추출해 메모리에 저장한다.
- 신규 유저(`registration_token` 파라미터 수신 시)는 FE 온보딩 경로로 리다이렉트한다.

### chapchu-auth 쪽 변경

- `chapchu-api` Confidential Client 등록 (PKCE 불필요, `CLIENT_SECRET_BASIC`).
- callback redirect_uri: `https://api.chapchu.site/auth/callback`, `http://localhost:8080/auth/callback`.
- `chapchu-front` 공개 클라이언트 제거.

## 에이전트 행동 지침

- chapchu-api에 토큰 발급 로직을 추가하지 마라. chapchu-auth 전담이다.
- `AuthController`는 OAuth 흐름 조율만 한다. 유저 생성·조회는 도메인 서비스에서 담당한다.
- 새 OAuth 엔드포인트를 추가할 때 `SecurityConfig.PUBLIC_PATHS`에 반드시 등록해야 인증 필터를 통과한다.
- refresh_token은 반드시 HttpOnly 쿠키로 관리한다. 응답 body에 담지 마라.
