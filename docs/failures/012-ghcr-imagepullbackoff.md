# 012. GHCR 프라이빗 이미지 — k3s ImagePullBackOff

## 증상
```
Failed to pull image "ghcr.io/chapchu-trip-platform/chapchu-api:latest": 
rpc error: ... 403 Forbidden
```
Pod가 `ImagePullBackOff` 상태로 기동 불가.

## 원인
k3s는 기본적으로 프라이빗 컨테이너 레지스트리 인증 정보가 없다.
`k8s/chapchu-api.yml`에 `imagePullSecrets` 없이 배포하면 GHCR 403 반환.

## 해결
1. 서버에서 `ghcr-secret` 생성 (PAT에 `read:packages` 권한 필수):
```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<GitHub username> \
  --docker-password=<PAT> \
  -n chapchu
```
2. `k8s/chapchu-api.yml` Deployment spec에 추가:
```yaml
spec:
  template:
    spec:
      imagePullSecrets:
        - name: ghcr-secret
```

## 에이전트 행동 지침
- `k8s/chapchu-api.yml` 수정 시 `imagePullSecrets` 블록을 절대 삭제하지 마라.
- PAT는 `read:packages` 스코프가 반드시 체크되어 있어야 한다.
