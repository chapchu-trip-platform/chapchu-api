# 018. RSA 키 환경변수 포맷 오류 — PEM 헤더 포함 base64 vs 순수 DER base64

## 증상
```
java.lang.IllegalStateException: AUTH_RSA_PRIVATE_KEY/AUTH_RSA_PUBLIC_KEY 파싱 실패
Caused by: java.io.IOException: extra data at the end
```
chapchu-auth 파드가 CrashLoopBackOff, RSA 키 파싱 단계에서 실패.

## 원인
`Jwks.java`는 `Base64.getDecoder().decode()`로 디코딩한 바이트를 곧바로
`PKCS8EncodedKeySpec` / `X509EncodedKeySpec`에 넘긴다.
즉 **순수 DER 바이트를 Base64 인코딩한 값**을 기대한다.

그런데 Secret 생성 시 아래 명령을 사용했다:
```bash
# 잘못된 방법
openssl pkcs8 -topk8 -nocrypt -in rsa_private.pem | base64 -w0
```
이 명령은 PEM 전체(헤더 + 내부 base64 + 푸터)를 다시 base64로 인코딩한다.
결과적으로 **double-base64 + PEM 헤더 포함** 상태가 되어 파싱 실패.

## 해결
`-outform DER` 플래그로 헤더 없는 순수 DER 바이트를 출력 후 base64 인코딩:
```bash
# 올바른 방법
AUTH_RSA_PRIVATE_KEY=$(openssl pkcs8 -topk8 -nocrypt -in rsa_private.pem -outform DER | base64 -w0)
AUTH_RSA_PUBLIC_KEY=$(openssl rsa -in rsa_private.pem -pubout -outform DER 2>/dev/null | base64 -w0)
```

## 에이전트 행동 지침
- `Jwks.java`처럼 `Base64.getDecoder().decode()` → `PKCS8EncodedKeySpec` 패턴을 쓰는 코드에
  RSA 키를 환경변수로 주입할 때는 반드시 `-outform DER | base64` 조합을 사용한다.
- PEM 포맷(`-----BEGIN PRIVATE KEY-----` 포함)을 그대로 base64로 인코딩하면 안 된다.
- Secret 재생성 시 기존 Secret 먼저 삭제 후 생성: `kubectl delete secret` → `kubectl create secret`
