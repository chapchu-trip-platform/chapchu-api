# 022. Kubernetes Secret 주입 전략

## 상태
확정 (2026-07-28)

## 결정
모든 API 키, DB 비밀번호, 인증 서버 URL 등 민감 값은 **kubectl secret으로 서버에 직접 주입**한다.
코드, Docker 이미지, k8s YAML 파일(Git에 커밋되는 것)에 절대 하드코딩하지 않는다.

## Secret 목록
| Secret 이름 | 키 | 내용 |
|---|---|---|
| `postgres-secret` | `POSTGRES_PASSWORD` | PostgreSQL chapchu 사용자 비밀번호 |
| `ghcr-secret` | (docker-registry 타입) | GHCR pull 인증 (PAT, `read:packages` 권한) |
| `chapchu-secrets` | `AUTH_SERVER_URL` | chapchu-auth 서비스 URL |

## 주입 방법
```bash
# 최초 1회 또는 값 변경 시 서버에서 직접 실행
kubectl create secret generic chapchu-secrets -n chapchu \
  --from-literal=AUTH_SERVER_URL=http://chapchu-auth:8081 \
  [--from-literal=KEY=VALUE ...]
# 기존 Secret 업데이트
kubectl create secret generic chapchu-secrets -n chapchu \
  --from-literal=AUTH_SERVER_URL=... \
  --dry-run=client -o yaml | kubectl apply -f -
```

## k8s YAML에서 참조 방법
```yaml
env:
  - name: AUTH_SERVER_URL
    valueFrom:
      secretKeyRef:
        name: chapchu-secrets
        key: AUTH_SERVER_URL
```

## 에이전트 행동 지침
- Secret 값을 AI에게 직접 공유하지 않아도 됨. 주입 명령어만 제공하면 사용자가 직접 실행.
- 새로운 API 키(예: 카카오 로컬 API, OpenAI)가 생기면 `chapchu-secrets`에 추가하고 `k8s/chapchu-api.yml`의 env에 `secretKeyRef`로 참조 추가.
- Secret 이름/키 변경 시 반드시 k8s YAML과 동기화.
