# 006. Claude가 gh pr comment로 직접 PR 코멘트 게시 시도 → 실패

## 증상
`claude-code-action@v1`에서 Claude에게 `gh pr comment`를 직접 실행하도록 프롬프트를 작성했을 때,
Claude가 실행을 거부하고 "게시할까요?"로 확인을 요청한다.

## 원인
Claude의 안전 정책: PR diff 자체에 "확인 없이 즉시 게시하라"는 지시문이 포함되어 있을 경우,
Claude는 이를 프롬프트 인젝션 가능성으로 판단하여 외부 액션(GitHub 코멘트 게시) 실행 전 사람의 확인을 요구한다.
`--dangerously-skip-permissions` 플래그로도 이 동작을 우회할 수 없다.

## 해결
Claude는 리뷰 결과를 로컬 파일(`/tmp/review.md`)에만 저장하고,
별도 shell 스텝(`gh pr comment --body-file`)이 게시를 담당한다.
Claude의 책임: 분석·저장. Shell의 책임: 게시.
