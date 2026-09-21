# 048 — 토큰은 ACTIVE 계정에만 넘긴다 (BFF 차단)

## 상태

- [x] 확정됨 (Accepted)

## 결정

`users.account_status`가 `ACTIVE`인 계정에만 토큰을 넘긴다. 아니면 **403**을 반환한다.

- `GET /auth/callback` — 코드 교환 후 비ACTIVE면 받은 refresh_token을 폐기하고 403
- `POST /auth/refresh` — 갱신 후 비ACTIVE면 refresh_token을 폐기하고 쿠키를 지운 뒤 403

판정은 `UserService.isActive(UUID)`가 한다. `ACTIVE`일 때만 true이고, **유저 행이 없거나
`account_status`가 NULL이어도 false**다(fail-closed).

## 왜 BFF에서 막는가

토큰을 실제로 만드는 곳은 `chapchu-auth`다(decisions/008). chapchu-api는 OAuth2 Confidential
Client로서 대신 호출해 주는 BFF다(decisions/033). 그래서 이 저장소에서는 **발급 자체를 막을 수 없고**,
받은 토큰을 폐기하고 FE로 넘기지 않는 방식으로 차단한다.

인가 코드만으로는 그게 누구인지 알 수 없어, 교환이 끝난 뒤 access_token의 `sub`
클레임(= `users.user_id`, decisions/016)을 읽어야 판정이 가능하다. 따라서 순서는 다음과 같다.

```
코드 교환 → 토큰 발급됨 → access_token 디코드 → sub로 users 조회
→ ACTIVE 아니면 → refresh_token revoke + 403
```

client_secret을 서버가 쥐고 있어 브라우저가 직접 chapchu-auth에서 토큰을 받아갈 수는 없으므로,
FE 경로에 대해서는 실효성이 있다.

## 한계 (알려진 것)

**완전한 차단은 `chapchu-auth`에서 해야 한다.** 여기서 막는 것은 "만들어진 토큰을 넘기지 않는 것"이지
"만들지 않는 것"이 아니다. chapchu-auth를 직접 호출할 수 있는 경로가 생기면 이 게이트는 우회된다.

~~**이미 발급된 access_token은 막지 못한다.**~~ → **decisions/049에서 닫았다.**
`CurrentUserIdArgumentResolver`가 유저 id를 만들 때 여기와 **같은 `isActive`** 로 계정 상태를
확인해, 비ACTIVE 계정의 토큰은 서명이 유효해도 401(`WITHDRAWN_ACCOUNT`)이다.

## account_status 컬럼

`V1__init_schema.sql`:

```sql
account_status VARCHAR(20) DEFAULT 'ACTIVE',
```

DEFAULT는 `ACTIVE`가 맞지만 **`NOT NULL`이 아니다.** DEFAULT는 INSERT에서 컬럼을 생략했을 때만
적용되므로 명시적으로 NULL을 넣으면 NULL이 저장된다. 스키마를 바꾸는 대신 코드에서 fail-closed로
처리했다 — 상태를 확인할 수 없는 계정은 통과시키지 않는다.

→ **V38에서 `NOT NULL`을 부여했다**(NULL은 `ACTIVE`로 백필). 매 요청 계정 상태를 보게 되면서
값이 항상 있어야 했기 때문이다(decisions/049). `isActive`의 fail-closed 방어는 그대로 둔다.

## 에이전트 행동 지침

- 이 저장소에 토큰 **발급** 로직을 추가하지 마라(decisions/008). 여기서는 전달 여부만 결정한다.
- 계정 상태 판정은 `UserService.isActive`를 쓰고, 컨트롤러에서 `account_status`를 직접 비교하지 마라.
- `AccountStatus`에 값을 추가할 때 `isActive`는 손댈 필요가 없다 — ACTIVE만 통과시키는 화이트리스트다.
