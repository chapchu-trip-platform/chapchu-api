# 029. Google OAuth redirect-uri 명시적 고정

## 상태
- [x] 확정됨 (Accepted)

## 결정
`application.yml`의 Google OAuth2 registration에 `redirect-uri`를 환경변수로 명시한다.

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            redirect-uri: "${GOOGLE_REDIRECT_URI:http://localhost:9000/login/oauth2/code/google}"
```

프로덕션 configmap에 `GOOGLE_REDIRECT_URI: "https://auth.chapchu.site/login/oauth2/code/google"`을 설정한다.

## 배경
Cloudflare Flexible SSL + Traefik 환경에서 `server.forward-headers-strategy: FRAMEWORK`만으로는
`X-Forwarded-Proto: https`가 Spring Boot까지 전달되지 않는다.
Traefik이 Cloudflare→Traefik 구간의 프로토콜(HTTP)을 기준으로 헤더를 덮어쓰기 때문이다.
결과적으로 Spring Security가 생성하는 redirect_uri가 `http://`가 되어 Google에서 mismatch 오류 발생.
헤더 의존을 없애고 URI를 명시적으로 고정하는 방식이 더 안전하다.

## 에이전트 행동 지침
- Cloudflare + Traefik 스택에서 OAuth redirect URI는 항상 명시적 환경변수로 지정한다.
- `{baseUrl}` 템플릿 변수는 역방향 프록시가 여러 겹일 때 신뢰할 수 없다.
- Google Cloud Console 등록 URI와 `GOOGLE_REDIRECT_URI` 값이 반드시 일치해야 한다.
