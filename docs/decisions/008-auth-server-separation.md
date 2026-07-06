# 008. 인증 서버 분리: chapchu-auth (별도 레포)

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 인증 서버(chapchu-auth)를 별도 레포의 독립 Spring Boot 프로젝트로 구성
- chapchu(메인 API)는 OAuth2 Resource Server로만 동작 — JWT 서명 검증만 수행
- 기술 스택: Spring Authorization Server

## 흐름
```
앱 → Google OAuth 2.0 로그인
         ↓
chapchu-auth  ← Spring Authorization Server (port 9000)
  - Google 토큰 검증
  - 자체 JWT 발급 (Access + Refresh Token)
  - JWKS 엔드포인트 노출
         ↓ (JWT)
chapchu API   ← OAuth2 Resource Server
  - JWKS로 JWT 서명 검증만 수행 (chapchu-auth 호출 없음, stateless)
```

## 이유
- 별도 프로세스로 진짜 네트워크 통신 학습
- OAuth2/OIDC 표준 프로토콜 흐름 직접 구현 경험
- MSA 전환 시 auth 서버가 가장 먼저 독립하는 컴포넌트
- k3s에 pod 2개로 배포 연습

## 에이전트 행동 지침
- chapchu 메인 API에 토큰 발급 로직을 추가하지 마라.
- JWT 검증은 Spring Security OAuth2 Resource Server 자동 설정에 맡겨라.
- `application.yml`의 `spring.security.oauth2.resourceserver.jwt.issuer-uri`가 chapchu-auth URL을 가리켜야 한다.
