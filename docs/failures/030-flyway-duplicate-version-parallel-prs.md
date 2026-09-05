# 030. 병렬 PR이 같은 Flyway 버전 번호를 선점

## 상황
서로 다른 두 PR이 각각 `V21` 마이그레이션을 들고 `dev`에 머지됐다.

```
V21__make_posts_photo_id_nullable.sql   ← #189
V21__restructure_travel_courses.sql     ← #187
```

Flyway는 같은 버전이 둘이면 부팅을 거부한다.

```
FlywayException: Found more than one migration with version 21
```

`dev`가 배포되면 앱이 아예 뜨지 않는다. 같은 유형이 #134, #138에 이어 세 번째다.

## 원인
버전 번호는 **PR 작성 시점**에 정해지는데 충돌은 **머지 시점**에 발생한다. 두 PR이 열려 있는
동안 각자 "현재 최신 + 1"을 골랐고, 둘 다 그 시점 기준으로는 맞았다.

`spring.flyway.outOfOrder`가 설정돼 있지 않아 기본값 `false`다. 순서를 벗어난 적용도 막히므로
번호를 나중에 끼워 넣는 것도 불가능하다.

`./gradlew check`는 이 충돌을 잡지 못한다. 마이그레이션 파일명은 컴파일 대상이 아니고
테스트 DB(H2)는 엔티티에서 스키마를 만들기 때문이다.

## 대응

한쪽 파일명을 다음 번호로 바꾼다. 파일 내용은 그대로 두므로 체크섬 문제가 없다.

**어느 쪽을 옮길지가 이 사고의 핵심이다. 반드시 DB를 먼저 봐라.**

```sql
SELECT version, description, checksum, success
FROM flyway_schema_history
WHERE version = '<중복된 번호>';
```

- **기록이 있으면** → 그 description에 해당하는 파일을 **그 번호에 그대로 둔다.** 다른 쪽을 옮긴다
- **기록이 없으면** → 어느 쪽을 옮겨도 된다

이미 적용된 쪽을 옮기면 코드의 파일명과 `flyway_schema_history`의 기록이 어긋나 여전히 기동에
실패한다. **중복을 고쳤는데 앱이 계속 안 뜨는 상태**가 되므로 처음보다 나쁘다.

### 실제로 이 판단을 틀렸다 (#193 → #195)

#191을 고칠 때 "#187이 더 큰 변경이고 먼저 머지됐으니 그쪽 V21을 남긴다"고 판단해
`make_posts_photo_id_nullable`을 V22로 옮겼다(#193). 틀렸다.

**머지 순서와 배포 순서가 달랐다.** #189가 먼저 배포되어 DB에는 이미
`V21 = make_posts_photo_id_nullable`이 기록돼 있었다. 그래서 #195에서 다시 뒤집어야 했다.

| | #193 (틀림) | #195 (맞음) |
|---|---|---|
| `make_posts_photo_id_nullable` | V22로 이동 | **V21 유지** (DB 기록과 일치) |
| `restructure_travel_courses` | V21 유지 | **V22로 이동** |

**판단 기준은 "어느 PR이 더 크냐"도 "어느 쪽이 먼저 머지됐냐"도 아니다. "DB에 무엇이 기록돼
있느냐" 하나뿐이다.** 머지 순서는 배포 순서를 보장하지 않는다.

DB 접근이 안 되면 추측하지 말고 접근 가능한 사람에게 물어라.

## 재발 방지
- 마이그레이션이 포함된 PR은 **머지 직전에** 번호 선점 여부를 다시 확인한다.
  ```
  git fetch origin
  git ls-tree -r origin/dev --name-only app/src/main/resources/db/migration/
  ```
- 원격 전 브랜치까지 훑으려면:
  ```
  for b in $(git ls-remote --heads origin | awk '{print $2}' | sed 's|refs/heads/||'); do
    git ls-tree -r "origin/$b" --name-only app/src/main/resources/db/migration/
  done | sort -u | tail
  ```
- `CLAUDE.md`의 "Flyway 마이그레이션 번호" 항목을 머지할 때마다 갱신한다.
- 마이그레이션 PR은 다른 마이그레이션 PR과 동시에 열어 두지 않는 편이 안전하다.
