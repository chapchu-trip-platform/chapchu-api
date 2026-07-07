# 013. Claude AI 자동 PR 리뷰 및 필수수정 자동 fix PR 생성
## 상태
- [x] 확정됨 (Accepted)
## 결정
- PR이 `dev` 또는 `main`으로 올라올 때 `claude-code-action@v1`이 자동으로 하네스 엔지니어링 기준 리뷰를 수행한다.
- Claude는 리뷰 결과를 `/tmp/review.md`에 저장하고, 별도 shell 스텝이 PR 코멘트로 게시한다.
- 🔴 필수수정 항목 발견 시 `fix/claude-{pr번호}` 브랜치에 수정 커밋 후 fix PR을 자동 생성한다.
- `fix/claude-*` 브랜치로 열린 PR은 리뷰 워크플로우에서 제외한다 (무한 루프 방지).
- Claude는 직접 GitHub에 코멘트를 게시하지 않는다 (보안: 프롬프트 인젝션 방지).
## 에이전트 행동 지침
- PR 리뷰 시 레이어 규칙·패키지 위치·PK 정책·TDD·Flyway·코드 품질 6가지 기준 필수 체크.
- 리뷰 결과는 반드시 `/tmp/review.md`에 Write 도구로 저장. 직접 `gh pr comment` 사용 금지.
- 🔴 항목 있으면 `/tmp/has_critical.txt`에 "true" 저장.
- fix PR 브랜치명: `fix/claude-{pr번호}-{timestamp}`.
