# 012. 버전 관리: HeadVer

## 상태
- [x] 확정됨 (Accepted)

## 결정
형식: `{Head}.{YearWeek}.{Build}`

| 자리 | 설정 | 의미 |
|---|---|---|
| Head | 수동 | 사용자 대상 릴리스 횟수. 팀 합의 후 증가 |
| YearWeek | 자동 | ISO 연도 2자리 + 주차 2자리 (예: `2627` = 2026년 27주) |
| Build | 자동 | CI 환경변수 `BUILD_NUMBER`. 로컬은 `0` |

예시: `1.2627.42` = 첫 번째 릴리스, 2026년 27주차, 빌드 42번

## 이유
- SemVer는 API 호환성 중심 — 사용자 서비스 배포에 부적합
- HeadVer는 언제 배포됐는지, 몇 번 배포됐는지를 버전에서 바로 파악 가능
- YearWeek, Build 자동화로 버전 협의 비용 제거

## 참고
https://techblog.lycorp.co.jp/ko/headver-new-versioning-system-for-product-teams

## 에이전트 행동 지침
- `build.gradle`의 `headVerHead` 값만 수동 관리. YearWeek, Build는 건드리지 마라.
- 사용자 대상 릴리스(main → 배포) 시 `headVerHead`를 1 증가시켜라.
- 개발 중 버전은 의미 없다. Head 증가는 실제 배포 시에만 한다.
