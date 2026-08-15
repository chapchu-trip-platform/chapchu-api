---
title: "025 - JWT 검증 시 JWKS 엔드포인트 hairpin NAT 문제"
date: 2026-07
status: resolved
---

## 현상

BFF 패턴 전환(033) 후 배포 환경에서 Bearer token을 포함한 API 요청이 500을 반환했다.

```
GET /users/me  Authorization: Bearer <access_token>  →  500
```

로컬에서는 정상 동작. 배포에서만 발생.

## 원인

chapchu-api의 `spring.security.oauth2.resourceserver.jwt.issuer-uri`가
`https://auth.chapchu.site`(외부 도메인)로 설정돼 있었다.

JWT 검증 시 Spring이 JWKS 엔드포인트(`https://auth.chapchu.site/.well-known/jwks.json`)를
조회하는데, chapchu-api와 chapchu-auth가 **같은 k8s 클러스터** 안에 있다.

클러스터 내부에서 외부 도메인으로 나갔다가 다시 들어오는 hairpin NAT 경로가
k3s + Traefik 기본 설정에서 지원되지 않아 JWKS 조회가 타임아웃됐다.

## 해결

`AUTH_SERVER_URL` 환경변수를 클러스터 내부 서비스 DNS로 변경.

```yaml
# chapchu-api-configmap.yaml (변경 전)
AUTH_SERVER_URL: "https://auth.chapchu.site"

# 변경 후
AUTH_SERVER_URL: "http://chapchu-auth-service.chapchu.svc.cluster.local:9000"
```

클러스터 내부 DNS는 hairpin NAT 없이 직접 통신한다.

## 재발 방지

- 같은 클러스터 내 서비스 간 통신은 외부 도메인 대신 **클러스터 내부 DNS**를 사용하라.
  형식: `http://{service-name}.{namespace}.svc.cluster.local:{port}`
- 새 서비스를 배포할 때 내부 서비스 간 URL과 외부 공개 URL을 ConfigMap에서 명확히 구분하라.
- 로컬 정상 + 배포 500 패턴이 나오면 네트워크 경로 차이부터 의심하라.
