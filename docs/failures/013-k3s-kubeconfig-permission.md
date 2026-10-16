# 013. k3s SSH 배포 — KUBECONFIG 권한 오류

## 증상
GitHub Actions SSH step에서:
```
error: error loading config file "/etc/rancher/k3s/k3s.yaml": 
open /etc/rancher/k3s/k3s.yaml: permission denied
```

## 원인
k3s의 기본 kubeconfig(`/etc/rancher/k3s/k3s.yaml`)는 root 소유로 일반 사용자(`ubuntu`) 접근 불가.
SSH Action은 `ubuntu` 사용자로 실행되므로 `kubectl` 명령이 모두 실패한다.

## 해결
서버에서 kubeconfig를 ubuntu 홈 디렉토리로 복사:
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown ubuntu:ubuntu ~/.kube/config
```

GitHub Actions SSH step script 첫 줄에 반드시 추가:
```bash
export KUBECONFIG=/home/ubuntu/.kube/config
```

## 에이전트 행동 지침
- `deploy.yml`의 SSH step script에서 `export KUBECONFIG=/home/ubuntu/.kube/config` 라인을 절대 삭제하지 마라.
- 새 서버 세팅 시 kubeconfig 복사 + chown이 선행되어야 CI/CD가 작동한다.
