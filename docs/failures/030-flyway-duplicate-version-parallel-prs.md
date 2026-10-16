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
아직 적용되지 않은 쪽의 파일명을 다음 번호로 바꾼다. 파일 내용은 그대로 두므로 체크섬 문제가 없다.

```
git mv V21__make_posts_photo_id_nullable.sql V22__make_posts_photo_id_nullable.sql
```

**이미 적용된 DB가 있다면 리네임만으로는 부족하다.** `flyway_schema_history`에 이전 버전으로
기록이 남아 있어 불일치가 난다. 이 경우 #138과 같은 절차가 필요하다.

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
