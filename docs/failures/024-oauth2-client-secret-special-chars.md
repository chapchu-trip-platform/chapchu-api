---
title: "024 - OAuth2 client secret에 base64 특수문자(+, =) 포함 시 인증 실패"
date: 2026-08-11
status: resolved
---

## 현상

chapchu-api가 chapchu-auth `/oauth2/token`에 코드 교환 요청을 보낼 때
`Authorization: Basic base64(clientId:clientSecret)` 헤더를 올바르게 사용했는데도
chapchu-auth 로그에 `client_secret does not match for registered client` 오류 발생.

- 두 파드의 `CHAPCHU_API_CLIENT_SECRET` 환경변수를 `od -c`, `md5sum`, `wc -c`로 비교 → 완전히 동일
- `CLIENT_SECRET_POST`(body) → `CLIENT_SECRET_BASIC`(header) 전환(PR #79)도 완료된 상태
- DEBUG 로그에서 `invalid_client` 확인됐지만 값이 왜 다른지 로그만으로 알 수 없었음

## 원인

`CHAPCHU_API_CLIENT_SECRET` 값이 `openssl rand -base64 32`로 생성돼 있었고,
이 값에 `+`와 `=` 문자가 포함되어 있었다 (예: `OfHQ7WvGaU1Nw5+TainJu1M8xXwO5MGUTSfYWVOHaGY=`).

Spring Security / Spring Authorization Server 내부에서 해당 값을 처리하는 과정 중
어느 레이어에서 `+`가 손상되거나 다르게 해석된 것으로 추정.
정확한 레이어는 디버그 로그 추가 없이는 확인 불가.

## 해결

client secret을 `openssl rand -hex 24`로 재생성해 alphanumeric(0-9, a-f)만 사용.

```bash
NEW_SECRET=$(openssl rand -hex 24)
kubectl patch secret chapchu-api-secret -n chapchu --type='merge' \
  -p "{\"data\":{\"CHAPCHU_API_CLIENT_SECRET\":\"$(echo -n "$NEW_SECRET" | base64 | tr -d '\n')\"}}"
kubectl patch secret chapchu-auth-secret -n chapchu --type='merge' \
  -p "{\"data\":{\"CHAPCHU_API_CLIENT_SECRET\":\"$(echo -n "$NEW_SECRET" | base64 | tr -d '\n')\"}}"
kubectl rollout restart deployment/chapchu-api deployment/chapchu-auth -n chapchu
```

GitHub Actions secret `CHAPCHU_API_CLIENT_SECRET`도 동일 값으로 업데이트해야
다음 CD에서 덮어쓰이지 않는다.

## 재발 방지

**client secret은 반드시 `openssl rand -hex N` 으로 생성한다.**
`openssl rand -base64 N`은 사용하지 않는다.
