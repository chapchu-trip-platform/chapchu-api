# 034. k8s 환경변수를 ConfigMap과 Secret으로 분리

## 배경

초기 배포에서는 모든 환경변수를 하나의 Secret에 몰아넣었다.
비밀이 아닌 설정값(CORS 허용 도메인, FE 리다이렉트 URL 등)과 실제 비밀(DB 비밀번호, client_secret 등)이
같은 리소스에 섞여 있었다.

Secret은 base64 인코딩이지 암호화가 아니므로 불필요하게 비밀 취급되는 값이 많아지면 관리 부담이 커진다.
또한 git에 올리는 ConfigMap 예시 파일에 비밀 기본값이 노출될 위험이 있다.

## 결정

환경변수를 성격에 따라 두 리소스로 분리한다.

| 리소스 | 포함 대상 | git 관리 |
|---|---|---|
| `chapchu-api-configmap` | DB 호스트, 포트, 이름, FE URL, CORS 도메인, AUTH_SERVER_URL 등 비밀이 아닌 설정 | 전체 파일 커밋 가능 |
| `chapchu-api-secret` | DB 비밀번호, client_id, client_secret, RSA 키 등 비밀 값 | 파일 커밋 금지, `.gitignore` |

ConfigMap은 `k8s/chapchu-api-configmap.yaml`로 관리하며 PR에 포함한다.
Secret은 `k8s/chapchu-api-secret.yaml.example`(비어있는 예시)만 커밋하고 실제 파일은 로컬에만 둔다.

## 에이전트 행동 지침

- 새 환경변수를 추가할 때 반드시 비밀 여부를 먼저 판단하라.
- DB 비밀번호, API key, 토큰 서명 키 등은 Secret에 넣어라.
- 도메인, URL, 허용 목록처럼 공개해도 무방한 값은 ConfigMap에 넣어라.
- `*-secret.yaml` 파일은 절대 git에 커밋하지 마라. `.gitignore`에 등록되어 있다.
- ConfigMap 변경 시 파드 재시작이 자동으로 되지 않는다. 배포 시 `kubectl rollout restart` 필요.
