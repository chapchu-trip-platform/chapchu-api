# 020. Traefik이 Cloudflare의 X-Forwarded-Proto: https를 http로 덮어씀

## 증상
```
400 오류: redirect_uri_mismatch
redirect_uri: http://auth.chapchu.site/login/oauth2/code/google
```
`server.forward-headers-strategy: FRAMEWORK`를 추가해도 Spring Boot가 redirect URI를 `http://`로 생성.

## 원인
Cloudflare Flexible SSL 환경에서 트래픽 흐름:
```
Client → (HTTPS) → Cloudflare → (HTTP) → Traefik → (HTTP) → Spring Boot
```
Cloudflare는 `X-Forwarded-Proto: https`를 설정하지만,
Traefik은 자신이 받은 연결 프로토콜(HTTP)을 기준으로 `X-Forwarded-Proto: http`로 덮어씀.
Spring Boot의 `forward-headers-strategy: FRAMEWORK`는 Traefik이 덮어쓴 `http`를 신뢰하므로
redirect URI가 `http://`로 생성되어 Google Cloud Console에 등록된 `https://` URI와 mismatch.

## 해결
헤더 의존을 제거하고 redirect-uri를 환경변수로 명시 (decisions/025 참고):
```yaml
redirect-uri: "${GOOGLE_REDIRECT_URI:http://localhost:9000/login/oauth2/code/google}"
```

## 에이전트 행동 지침
- Cloudflare Flexible SSL + Traefik 조합에서 `X-Forwarded-Proto`는 신뢰할 수 없다.
- Traefik에서 Cloudflare IP를 `trustedIPs`로 등록하거나 Traefik Middleware로 헤더를 강제하지 않는 한
  `forward-headers-strategy`만으로는 HTTPS 감지가 불가하다.
- OAuth redirect URI, 외부 URL 생성 등 프로토콜이 중요한 값은 항상 환경변수로 명시한다.
