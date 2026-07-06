# Claude Code 첫 세션 프롬프트 (팀원용)

> chapchu-api 프로젝트를 처음 열었을 때 Claude Code에 아래 내용을 붙여넣어라.
> 이후 세션부터는 CLAUDE.md가 자동으로 로드되므로 별도 프롬프트 불필요.

---

```
안녕. 나는 chapchu-api 프로젝트 팀원이야.
이 프로젝트는 반려동물 동반 여행 플랫폼(PetTrip)의 Spring Boot 백엔드야.

세션 시작 전에 아래 파일들을 순서대로 읽고 프로젝트 컨텍스트를 파악해줘:

1. CLAUDE.md — 세션 규칙 및 체크리스트
2. AGENTS.md — 하네스 엔지니어링 규칙 전체
3. docs/decisions/ 폴더 내 모든 파일 — 기술 결정 001~010
4. docs/failures/ 폴더 내 모든 파일 — 안티패턴
5. docs/schema/init.sql — DB 스키마
6. git log --oneline -10 — 최근 커밋 맥락

읽은 후 아래를 알려줘:
- 내가 담당할 도메인 (auth / user / pet / photo)에서 현재 구현된 것과 남은 것
- 오늘 작업을 시작하기 위한 브랜치 생성 명령어
- 주의해야 할 하네스 규칙 요약 (3줄 이내)
```
