# 020. CI/CD: GitHub Actions → GHCR → k3s 자동 배포

## 상태
확정 (2026-07-28)

## 결정
`dev` 브랜치 push 시 GitHub Actions가 자동으로:
1. `./gradlew :app:bootJar -x test` 로 JAR 빌드
2. Docker 이미지 빌드 → GHCR(`ghcr.io/chapchu-trip-platform/chapchu-api:latest`) push
3. `k8s/chapchu-api.yml` 를 서버에 scp
4. SSH로 `kubectl apply` + `kubectl rollout restart` + `kubectl rollout status --timeout=4m`

## 파이프라인 파일
`.github/workflows/deploy.yml`

## 필수 GitHub Secrets
| Secret | 내용 |
|---|---|
| `SERVER_HOST` | Lightsail 정적 IP |
| `SERVER_USER` | `ubuntu` |
| `SERVER_SSH_KEY` | Lightsail SSH 개인키 (PEM) |

`GITHUB_TOKEN`은 Actions에서 자동 제공 (GHCR push 권한 포함).

## Docker 이미지
- Base: `eclipse-temurin:25-jdk-alpine` (빌드) / `eclipse-temurin:25-jre-alpine` (런타임)
- 멀티스테이지 빌드로 최종 이미지에 JDK 미포함
- 이미지 태그: `:latest` 단일 태그 (k3s에서 `imagePullPolicy: Always` 기본값)

## k3s GHCR 인증
GHCR 프라이빗 이미지는 k3s가 기본적으로 pull 불가. 다음 Secret 필수:
```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<GitHub username> \
  --docker-password=<PAT with read:packages> \
  -n chapchu
```
Deployment의 `spec.template.spec.imagePullSecrets` 에 반드시 참조.

## 에이전트 행동 지침
- `k8s/chapchu-api.yml` 수정 시 `imagePullSecrets` 블록 유지 필수
- `deploy.yml`의 SSH step에 `command_timeout: 5m` 유지 (rollout이 기본 10분+ 걸릴 수 있음)
- `kubectl rollout status --timeout=4m` 으로 Action 시간 초과 방지
