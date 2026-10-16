# 023. secret.example.yaml이 실제 시크릿을 REPLACE_ME로 덮어씀

## 증상
```
Caused by: java.net.UnknownHostException: REPLACE_ME
```
chapchu-auth 파드가 CrashLoopBackOff. `kubectl patch`로 DB_URL을 수정해도 재배포하면 다시 `REPLACE_ME`로 돌아옴.

## 원인
`deploy.yml`이 `kubectl apply -f ~/k8s/chapchu-auth/k8s/`로 디렉터리 전체를 적용했다.
`k8s/secret.example.yaml`의 `metadata.name`이 실제 시크릿(`chapchu-auth-secret`)과 동일하고
확장자가 `.yaml`이어서 kubectl이 이를 실제 리소스로 인식, 매 배포마다 REPLACE_ME로 덮어씀.

배포 순서:
1. `Apply k8s secrets` 스텝 → 올바른 값으로 시크릿 생성 ✅
2. `Deploy to server` 스텝 → 디렉터리 전체 apply → `secret.example.yaml`이 덮어씀 ❌
3. rollout restart → 새 파드가 REPLACE_ME 읽고 죽음

## 해결
- `secret.example.yaml` → `secret.yaml.example`로 이름 변경 (kubectl이 `.yaml` 아닌 확장자 무시)
- `deploy.yml`을 디렉터리 전체 적용 대신 파일 단위로 명시: `namespace/configmap/service/deployment`만 apply

## 에이전트 행동 지침
- k8s 예시 파일은 반드시 `*.yaml.example` 형식으로 저장한다 (`ingress.yaml.example` 컨벤션 따름).
- `kubectl apply -f <디렉터리>/`는 예시 파일을 실수로 적용할 위험이 있으므로 사용하지 않는다.
- CI에서 시크릿과 일반 리소스 apply는 분리된 스텝으로 관리한다.
