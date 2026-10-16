---
title: "027 - Cloudflare Tunnel 환경에서 redirect_uri 동적 생성 시 HTTP로 생성됨"
date: 2026-07
status: resolved
---

## 현상

BFF OAuth 흐름에서 chapchu-auth가 redirect_uri 불일치 오류를 반환했다.

```
chapchu-auth → error: redirect_uri_mismatch
```

로컬에서는 정상 동작. Cloudflare Tunnel을 통한 배포 환경에서만 발생.

## 원인

`AuthController.buildRedirectUri()`가 `HttpServletRequest`에서 scheme·host·port를 조합해
redirect_uri를 동적으로 생성했다.

```java
return request.getScheme() + "://" + request.getServerName() + portPart + "/auth/callback";
// 결과: http://api.chapchu.site/auth/callback
```

Cloudflare Tunnel은 외부 HTTPS 요청을 k8s 파드로 전달할 때 **HTTP로 변환**한다.
`request.getScheme()`이 `http`를 반환해 redirect_uri가 `http://...`로 생성됐다.

chapchu-auth에 등록된 redirect_uri는 `https://api.chapchu.site/auth/callback`이므로 불일치.

`X-Forwarded-Proto: https` 헤더를 읽어 scheme을 보정하는 방법도 시도했지만,
Cloudflare → Traefik → 파드 경로에서 헤더 전달이 일관되지 않아 불안정했다.

## 해결

redirect_uri를 동적 생성하지 않고 환경변수로 고정한다.

```yaml
# application.yml
chapchu-api:
  auth:
    callback-url: ${AUTH_CALLBACK_URL:}
```

```java
private String buildRedirectUri(HttpServletRequest request) {
    if (configuredCallbackUrl != null && !configuredCallbackUrl.isEmpty()) {
        return configuredCallbackUrl;
    }
    // 환경변수 없을 때만 동적 생성 (로컬 개발용)
    ...
}
```

```yaml
# k8s/chapchu-api-configmap.yaml
AUTH_CALLBACK_URL: "https://api.chapchu.site/auth/callback"
```

## 재발 방지

- Cloudflare Tunnel / 리버스 프록시 뒤에서는 `request.getScheme()`을 신뢰하지 마라.
- OAuth redirect_uri처럼 **정확히 일치해야 하는** URL은 동적 생성을 피하고 환경변수로 고정하라.
- 로컬 개발에서는 환경변수를 비워두면 동적 생성 폴백으로 동작하도록 설계하면 환경 구분이 된다.
