# 024. chapchu-auth NodePort 30900 임시 외부 노출

## 상태
- [x] 확정됨 (Accepted) — 임시 결정, issue #37에서 교체 예정

## 결정
- chapchu-auth Service를 NodePort 30900으로 외부에 직접 노출
- AWS Lightsail 방화벽에 TCP 30900 인바운드 규칙 추가
- Google OAuth redirect URI와 AUTH_SERVER_URL 모두 포트 30900 포함

## 배경
- 초기 배포 완료를 우선시 — Ingress + SSL 설정은 별도 작업
- Cloudflare 프록시가 비표준 포트를 지원하지 않아 30900 포트를 직접 열어야 함
- 보안상 권장하지 않으나 개발/테스트 단계에서 허용

## 에이전트 행동 지침
- 이 결정은 임시다. issue #37 완료 시 다음을 수행해야 함:
  1. Service를 ClusterIP로 변경
  2. Ingress 리소스 추가 (443 포트)
  3. Lightsail 방화벽에서 30900 규칙 제거
  4. Google OAuth redirect URI를 `https://` 로 갱신
  5. AUTH_SERVER_URL을 `https://auth.chapchu.site`로 갱신
