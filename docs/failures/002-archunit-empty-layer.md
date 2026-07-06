# 002. ArchUnit 초기 빈 레이어 검사 실패

## 실패 원인
- `layeredArchitecture()` 에 정의된 레이어(Controller, Service, Repository, Model)에 실제 클래스가 하나도 없을 때 ArchUnit 이 위반으로 판단한다.
- 오류: `Layer 'Controller' is empty` 등 4개 레이어 전부 실패.
- `slices().matching(...)` 및 `noClasses().that()...` 도 매칭 클래스가 없으면 `failOnEmptyShould` 기본값으로 실패.

## 해결책 (현재 적용)
- `layeredArchitecture()` 체인에 `.withOptionalLayers(true)` 추가.
- `src/test/resources/archunit.properties` 에 `archRule.failOnEmptyShould=false` 설정.

## 에이전트 행동 지침
- `withOptionalLayers(true)` 와 `archunit.properties` 설정을 제거하지 마라.
- 비즈니스 클래스가 추가되면 ArchUnit 이 자동으로 실제 위반을 감지하기 시작한다.
