---
title: "028 - SecurityConfig 두 개의 필터 체인으로 공개 엔드포인트 401 회피 시도 실패"
date: 2026-08
status: reverted
---

## 현상

공개 엔드포인트(`/breeds`, `/activities` 등)에 **유효하지 않은 JWT**를 담아 요청하면 401이 반환됐다.

```
GET /breeds  Authorization: Bearer <만료된 토큰>  →  401
GET /breeds  (토큰 없음)                          →  200  ← 의도된 동작
```

토큰이 없을 때는 200이지만, 잘못된 토큰이 있을 때 401이 나는 건 의도와 달랐다.
FE가 토큰 만료 여부와 무관하게 공개 API를 호출할 수 있어야 했다.

## 시도한 해결책

Spring Security의 두 개의 `SecurityFilterChain` 빈을 사용해 경로별로 다른 체인을 적용했다.

```java
@Bean @Order(1)  // 공개 경로: JWT 검증 없음
public SecurityFilterChain publicFilterChain(HttpSecurity http) { ... }

@Bean @Order(2)  // 인증 경로: JWT 검증 있음
public SecurityFilterChain authenticatedFilterChain(HttpSecurity http) { ... }
```

## 왜 실패했나

두 필터 체인은 **경로 기반으로 분리**되지만 Spring Security의 CORS 설정이 체인마다 독립적으로 구성된다.
두 번째 체인의 CORS 설정이 누락되거나 충돌해 인증 경로에서 CORS 오류가 발생했다.

또한 `oauth2ResourceServer` 설정이 체인마다 독립적으로 구성돼 JWKS 조회가 중복됐고,
`@WebMvcTest` 기반 테스트에서 어떤 체인이 로드될지 예측이 어려워 테스트가 깨졌다.

## 해결 (원래대로 복구)

두 체인 접근을 버리고 단일 `SecurityFilterChain`을 유지했다.

```java
auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
    .requestMatchers(PUBLIC_PATHS).permitAll()
    .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
    .anyRequest().authenticated()
```

FE가 공개 API 호출 시 잘못된 토큰을 함께 보내지 않도록 FE 측에서 처리하는 것으로 합의했다.

## 재발 방지

- Spring Security 필터 체인을 두 개로 분리하는 건 복잡도 대비 이점이 작다.
  특히 `@WebMvcTest`와 궁합이 나빠 테스트가 불안정해진다.
- 공개 엔드포인트의 JWT 관련 동작을 바꾸려면 필터 체인을 추가하지 말고
  **`permitAll()` 경로 조정** 또는 **`AuthenticationEntryPoint` 커스터마이징**으로 해결하라.
- 단일 체인 원칙을 유지하라.
