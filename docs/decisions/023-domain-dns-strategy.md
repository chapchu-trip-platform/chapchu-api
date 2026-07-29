# 023. 도메인 및 DNS 전략

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 도메인: `chapchu.site` (가비아 구매)
- DNS: Cloudflare (무료 플랜) — 가비아 네임서버를 Cloudflare로 교체
- A 레코드: `auth.chapchu.site` → `13.125.127.196` (DNS only, 프록시 비활성)
- 현재 접근 구조: `http://auth.chapchu.site:30900` (HTTP, NodePort)

## 배경
- Google OAuth redirect URI에 IP 주소 직접 입력 불가 — 도메인 필수
- Cloudflare를 선택한 이유: 무료 SSL, CDN, DDoS 방어를 동시에 제공
- 현재 NodePort 30900을 사용하므로 Cloudflare 프록시 비활성화 (프록시는 80/443만 지원)
  - 프록시 활성화 및 HTTPS 전환은 issue #37(Ingress + SSL 설정)에서 진행

## 에이전트 행동 지침
- `AUTH_SERVER_URL` 관련 설정값은 `http://auth.chapchu.site:30900` 기준
- Google OAuth redirect URI: `http://auth.chapchu.site:30900/login/oauth2/code/google`
- Ingress + SSL 작업(issue #37) 완료 후 전체 URL을 `https://auth.chapchu.site`로 갱신해야 함
- Cloudflare DNS 레코드 변경이 필요하면 `auth` A 레코드를 수정 (DNS only 유지 중)
