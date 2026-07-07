# 013. Claude AI 자동 PR 리뷰 파이프라인
## 상태
- [x] 확정됨 (Accepted)

## 결정
- PR이 `dev` 또는 `main`으로 올라올 때 `claude-code-action@v1`이 자동으로 하네스 엔지니어링 기준 리뷰를 수행한다.
- Claude는 리뷰 결과를 `/tmp/review.md`에 저장하고, 별도 shell 스텝이 PR 코멘트로 게시한다.
- 🔴 필수수정 항목 발견 시, 개발자가 Claude Code 세션에서 직접 수정을 요청한다.
- 자동 fix PR 생성은 하지 않는다 (워크플로우 파일 push 권한 문제 + 수정 품질 저하).
- Claude는 직접 GitHub에 코멘트를 게시하지 않는다 (보안: 프롬프트 인젝션 방지).

## 에이전트 행동 지침
- PR 리뷰 시 레이어 규칙·패키지 위치·PK 정책·TDD·Flyway·코드 품질 6가지 기준 필수 체크.
- 리뷰 결과는 반드시 `/tmp/review.md`에 Write 도구로 저장. 직접 `gh pr comment` 사용 금지.

## 폐기된 접근법
- 자동 fix PR (two-step Claude action): `.github/workflows/` 수정 시 `workflows` 권한 없어 push 실패.
  수정 품질도 단일 Claude 세션(전체 컨텍스트 보유)이 CI headless 세션보다 훨씬 높다.
