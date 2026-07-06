# 002. 도메인-중심 패키지 구조

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 패키지 구조: `com.example.system.{domain}.{layer}`
- 예시: `com.example.system.user.controller.UserController`
- 레이어명: `controller` / `service` / `repository` / `model`
- `model` = 순수 도메인 Entity. 외부 레이어(controller, service, repository) 참조 금지.

## 에이전트 행동 지침
- 새 클래스 생성 시 반드시 `{도메인}.{레이어}` 2단계 하위 패키지에 위치시켜라.
- `domain`, `api`, `entity` 등 다른 레이어명을 사용하지 마라. 반드시 위 4가지만 사용.
- `model` 패키지 클래스에서 `controller`, `service`, `repository` 를 import 하지 마라.
