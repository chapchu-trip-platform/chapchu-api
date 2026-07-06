# 007. 프로젝트 구조: Modular Monolith + Gradle Multi-module

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 단일 Spring Boot 프로세스로 배포하되, Gradle 멀티모듈로 도메인 경계를 물리적으로 분리
- 모듈 구성: `app`, `module-common`, `module-user`, `module-place`, `module-trip`, `module-album`, `module-review`, `module-recommendation`

## 이유
- MSA 전환 시 각 모듈을 별도 서비스로 추출하기 쉬운 구조
- 지금은 단일 JVM → 네트워크 오버헤드 없음, 운영 복잡도 낮음
- 2인 팀 + 공모전 데드라인 → MSA 풀 구성은 오버엔지니어링
- k3s에 단일 pod로 배포 → 나중에 필요 시 pod 분리

## 모듈 의존성 방향
```
app → module-* (모두 참조)
module-recommendation → module-common
module-{user|place|trip|album|review} → module-common
module-common → (외부 라이브러리만)
```

## 에이전트 행동 지침
- 신규 클래스는 반드시 해당 도메인 모듈의 패키지에 생성하라.
- 모듈 간 순환 의존성을 만들지 마라 (ArchUnit이 감지).
- `app` 모듈에 비즈니스 로직을 추가하지 마라. `app`은 조립 진입점이다.
