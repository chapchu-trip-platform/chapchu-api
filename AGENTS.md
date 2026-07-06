# AI 에이전트 실행 하네스 및 팀 협업 제약 조건 (AGENTS.md)

너는 현재 엄격한 통제 장치(Harness Engineering)가 적용된 Spring Boot 프로젝트에서 2명 이상의 인간 개발자와 협업하는 AI 에이전트다.
코드를 작성하거나 수정하기 전에 반드시 본 문서의 규칙을 파악하고, 실행 루프 내내 이를 준수해야 한다.

## 1. 아키텍처 제약 조건 (엄격한 계층형 구조)
- **도메인:** Java / Spring Boot 백엔드 아키텍처.
- **계층 규칙:** `ArchUnit`과 `Checkstyle`을 통해 단방향 의존성 흐름을 강제한다. (`Controller` -> `Service` -> `Repository`)
- `Domain` (Entity) 계층은 외부 계층을 절대 참조해서는 안 되는 순수한 핵심 비즈니스 영역이다. 의존성을 직접 주입(Direct Injection)하는 등 꼼수로 우회하지 마라.
- **Strict API/Backend Only:** 이 프로젝트는 FE(프론트엔드)가 완전히 분리된 순수 백엔드 API 서버다. 어떠한 경우에도 화면 렌더링을 위한 코드(HTML, CSS, JS, React, Thymeleaf 등)를 생성하거나 제안하지 마라. 모든 클라이언트 통신은 JSON 기반의 REST API(또는 지정된 스펙)로만 이루어진다.

## 2. 지식 저장소 및 기술 결정 동기화
코드를 생성하기 전, 팀이 과거에 합의한 아키텍처 결정과 실패 경험을 반드시 숙지하라.
- **기술 결정 (`docs/decisions/`):** 팀이 선택한 인프라 합의안. 역행하는 기술을 도입하지 마라.
- **실패 기록 (`docs/failures/`):** 시도했다가 폐기된 안티패턴. 절대 제안하거나 코딩하지 마라.

## 3. 커밋 전 자동 검증 루프 (The Gauntlet)
코드를 완성하고 커밋을 시도하면 `pre-commit` 훅이 아래 명령어를 강제 실행한다:
`./gradlew spotlessApply checkstyleMain spotbugsMain pmdMain test`

빌드 실패(`Exit Code 1`) 발생 시, 반드시 아래의 **자율 수정 루프(Self-Correction Loop)**를 수행하라:
1. **Spotless:** `./gradlew spotlessApply`를 실행하여 포맷을 다듬어라.
2. **Checkstyle:** 텍스트 로그를 읽고 잘못된 import나 네이밍을 수정하라.
3. **SpotBugs:** 잠재적 결함(NullPointer 등)을 고쳐라.
4. **PMD:** 사용하지 않는 변수나 프라이빗 메서드를 과감히 삭제하라.
5. **ArchUnit:** `AgentHarnessArchitectureTest` 또는 `DriftPreventionTest` 실패 시 에러 메시지를 읽고 수정하라.

## 4. 다자간 팀 협업 및 Git Pull 동기화 규칙 (Multi-User Collaboration Protocol)
### [Step 1] `git pull` 직후 컨텍스트 파악 및 하네스/지식 저장소 동기화
1. **하네스 룰 및 지식 저장소 변경 감지:** `git pull` 직후 `AGENTS.md`, `build.gradle`, `config/` 등의 설정 파일뿐만 아니라 **`docs/decisions/` 및 `docs/failures/` 내의 지식 저장소 문서가 동료에 의해 추가되거나 변경되었는지 최우선으로 확인하라.** 변경 사항이 감지되었다면 **즉시 해당 문서를 읽고 제약 조건 메모리를 재동기화(Reload)**해야 한다.
2. **비즈니스 로직 맥락 파악:** `.harness/last_pull_summary.md` 및 `git log`를 확인하여 동료가 새로 추가한 DTO, 예외 처리 패턴 등을 파악하고 그 패턴에 일관되게 맞추어 코딩하라.

### [Step 2] 동료 코드 보호 및 스코프 제한
현재 사용자(나)가 지시한 과제와 직접적인 관련이 없는 동료의 코드를 '리팩토링' 명목으로 임의 수정하지 마라. 기존 퍼블릭 메서드 서명은 함부로 바꾸지 말고 안전하게 확장하라.

## 5. 가비지 컬렉션 및 드리프트 방지 규칙 (Anti-Drift Protocol)
- **임시 파일 금지:** 디버깅 목적의 임시 파일(`*temp*`, `*_old.java`, `*.bak`)을 만들지 마라.
- **죽은 코드 제거:** 안 쓰는 메서드나 변수는 주석으로 남기지 말고 삭제하라.
