---
title: "026 - OAuth2 토큰 교환에 CLIENT_SECRET_POST 방식 사용 시 인증 실패"
date: 2026-07
status: resolved
---

## 현상

BFF 패턴 전환(033) 초기 구현에서 authorization code → token 교환 요청이 chapchu-auth에서 거절됐다.

```
POST /oauth2/token  →  401 invalid_client
```

chapchu-api의 클라이언트 자격증명(`client_id`, `client_secret`)이 올바른데도 실패.

## 원인

`RestClient`로 직접 토큰 교환을 구현할 때 `client_id`와 `client_secret`을 요청 **body**에 포함시켰다.

```java
body.add("client_id", reg.getClientId());
body.add("client_secret", reg.getClientSecret());
```

이는 `CLIENT_SECRET_POST` 방식이다.

chapchu-auth(Spring Authorization Server)에 등록된 `chapchu-api` 클라이언트의
`clientAuthenticationMethod`가 `CLIENT_SECRET_BASIC`이었다.
서버가 허용하는 방식과 클라이언트가 보내는 방식이 달라 인증 실패.

## 해결

body 대신 `Authorization: Basic base64(clientId:clientSecret)` 헤더를 사용한다.

```java
String credentials = Base64.getEncoder().encodeToString(
    (reg.getClientId() + ":" + reg.getClientSecret()).getBytes(StandardCharsets.UTF_8));

restClient.post()
    .uri(...)
    .header("Authorization", "Basic " + credentials)
    // body에서 client_id, client_secret 제거
```

## 재발 방지

- chapchu-auth 클라이언트 등록 시 `clientAuthenticationMethod`를 명시하고,
  chapchu-api의 요청 방식과 반드시 일치시켜라.
- Spring Security OAuth2 Client 자동 구성(`spring-boot-starter-oauth2-client`)을 쓰면
  이 매칭을 자동으로 처리한다. `RestClient`로 직접 구현할 때는 수동으로 맞춰야 한다.
- `invalid_client` 오류가 나오면 client secret 값보다 **인증 방식 불일치**를 먼저 의심하라.
