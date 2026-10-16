# 019. 인프라: AWS Lightsail + k3s 단일 노드 배포

## 상태
확정 (2026-07-28)

## 배경
chapchu-api를 배포할 서버 환경이 필요했다. 초기에는 Oracle Cloud A1.Flex(ARM, 무료 티어)를 검토했으나
2026년 6월 Oracle이 무료 티어 스펙을 4 OCPU/24GB → 2 OCPU/12GB로 절반 축소하여 안정성 우려.

## 결정
**AWS Lightsail** (Ubuntu 24.04, `ap-northeast-2`, `$5/월` 플랜, 정적 IP: 13.125.127.196) +
**k3s** 단일 노드 Kubernetes 클러스터.

## 이유
- Lightsail은 트래픽 포함 고정 요금 → 예측 가능한 비용
- k3s: Kubernetes 호환 API를 유지하면서 단일 노드에서 경량 실행 (에이전트 별도 불필요)
- 나중에 멀티 노드로 확장하거나 EKS로 마이그레이션할 때 manifest 재사용 가능
- Traefik Ingress가 k3s에 기본 포함

## 클러스터 구조
```
namespace: chapchu
  Deployment: chapchu-api   (ghcr.io/chapchu-trip-platform/chapchu-api:latest)
  Deployment: postgres      (pgvector/pgvector:pg16)
  PVC:        postgres-pvc  (5Gi)
  Service:    chapchu-api   (ClusterIP, 8080)
  Service:    postgres      (ClusterIP, 5432)
Secrets (kubectl로 수동 주입, 코드/이미지에 절대 포함 금지):
  ghcr-secret       — GHCR pull 인증
  postgres-secret   — POSTGRES_PASSWORD
  chapchu-secrets   — AUTH_SERVER_URL (및 향후 API 키들)
```

## kubeconfig 위치
`/home/ubuntu/.kube/config` (k3s 기본 경로 `/etc/rancher/k3s/k3s.yaml`를 복사)
SSH 세션에서 `export KUBECONFIG=/home/ubuntu/.kube/config` 필수.

## 에이전트 행동 지침
- k8s 매니페스트는 `k8s/` 디렉토리에 관리
- Secret 값은 절대 코드/YAML에 하드코딩하지 말고 `secretKeyRef`로 참조
- 서버 직접 접근 필요 시 SSH 키(`~/.ssh/chapchu-lightsail.pem` 또는 사용자 지정 경로) 사용
