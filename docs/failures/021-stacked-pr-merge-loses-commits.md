# 021. 스택 PR을 base 브랜치에 머지하면 dev에 반영되지 않는다

## 증상

PR이 분명히 `MERGED` 상태인데, `dev`에는 해당 커밋이 하나도 없다. GitHub UI에도 에러가 없고 CI도 통과했으므로 아무도 유실을 알아채지 못한다.

두 번 반복해서 발생했다.

| 회차 | PR | base | 결과 |
|---|---|---|---|
| 1 | #22, #23, #24 | 서로의 feature 브랜치 | 57개 파일 / 약 2,341줄이 dev에 미반영 → #49로 복구 |
| 2 | #48 | `feature/yeonseung/security-jwt` (#46의 head) | `@CurrentUserId` 전체가 dev에 미반영 |

2회차는 특히 위험했다. `#46`(JWT 인증 강제)은 dev에 들어가 배포됐는데 `#48`(JWT의 `sub`을 실제로 읽는 코드)만 유실돼서, **인증은 요구하지만 로그인한 유저를 식별하지 못하는** 상태로 운영에 나갔다. 어떤 계정으로 로그인해도 컨트롤러는 `TempAuthContext.TEMP_USER_ID`(DB에 없는 하드코딩 UUID)를 써서 전부 404가 났다.

## 원인

PR A의 head 브랜치를 base로 삼아 PR B를 만든 뒤(스택 PR), **A를 먼저 머지하면 B의 base 브랜치는 이미 dev에 흡수돼 죽은 브랜치가 된다.** 이 상태에서 B를 머지하면 죽은 브랜치 위에 커밋이 얹힐 뿐 dev로는 전파되지 않는다.

2회차의 머지 시각이 이를 그대로 보여준다.

```
#46 merged: 05:13:23  → dev 반영 O
#48 merged: 05:13:37  → base(feature/yeonseung/security-jwt)에 반영, dev 반영 X
```

14초 차이다. 연속으로 머지 버튼을 누르면 그대로 재현된다.

GitHub이 base 브랜치 머지 시 자식 PR을 dev로 자동 retarget 해주는 건 **head 브랜치가 삭제될 때뿐**이다. 브랜치를 남겨두면 retarget이 일어나지 않는다.

## 해결

유실된 커밋을 dev 기반 새 브랜치로 체리픽해 다시 PR을 올린다.

```bash
git fetch origin
git checkout -b feature/{작업자}/{기능}-restore origin/dev
git cherry-pick {유실된_커밋}
./gradlew spotlessApply checkstyleMain spotbugsMain pmdMain test
```

체리픽 이후 dev에 새로 들어온 코드가 유실 커밋의 전제를 깨뜨릴 수 있으므로 **반드시 전체 게이틀릿을 돌려라.** 2회차 복구에서는 그 사이 dev에 추가된 컨트롤러 7개가 여전히 `TempAuthContext`를 참조해 컴파일이 깨졌고, `#53`이 추가한 테스트 2건은 jwt 후처리기가 없어 401로 실패했다.

## 에이전트 행동 지침

- **스택 PR을 만들지 마라.** 새 PR의 base는 언제나 `dev`로 하라. 선행 PR의 코드가 필요하면 base를 쌓지 말고 dev 머지를 기다리거나 rebase 하라.
- 불가피하게 스택 PR을 만들었다면, 부모 PR을 머지할 때 **head 브랜치 삭제를 반드시 함께 하라.** 그래야 자식 PR이 dev로 자동 retarget 된다.
- PR이 `MERGED`로 떴다고 반영을 단정하지 마라. **base 브랜치가 `dev`였는지 확인하라.**

  ```bash
  gh pr view {번호} --json baseRefName,mergedAt
  ```

- 머지 직후 dev에 실제로 들어갔는지 파일 단위로 검증하라.

  ```bash
  git fetch origin && git cat-file -e origin/dev:{추가된_파일_경로} && echo 반영됨
  ```
