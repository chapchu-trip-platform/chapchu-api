# 049 — 탈퇴 계정의 토큰은 매 요청 막는다 (@CurrentUserId 리졸버 차단)

## 상태

- [x] 확정됨 (Accepted)

## 결정

`users.account_status`가 `ACTIVE`가 아닌 계정의 access_token은 **서명이 유효하고 만료 전이어도**
인증이 필요한 모든 엔드포인트에서 **401**을 받는다.

```json
{ "code": "WITHDRAWN_ACCOUNT", "message": "사용할 수 없는 계정입니다." }
```

판정은 `UserService.isActive(UUID)`가 한다(발급 단계와 **같은 메서드**, decisions/048).
차단은 `CurrentUserIdArgumentResolver`가 `@CurrentUserId`를 만들 때 한다.

## 왜 필요했나

decisions/048은 **발급 단계**만 막았다. 그런데 access_token은 chapchu-auth가 서명한 JWT라
이 저장소에서 만료시킬 방법이 없다. 그래서 탈퇴 직전에 받아 둔 토큰이 만료 시각까지 살아 있었고,
탈퇴한 사람이 그 토큰으로 API를 계속 쓸 수 있었다.

특히 구멍이 컸던 경로:

```
DELETE /users/me                             → account_status = WITHDRAWN
PATCH  /users/me {"accountStatus":"ACTIVE"}  → 스스로 되살아남 (같은 토큰으로)
```

`User.update`에 가드가 없고 리소스 서버가 계정 상태를 보지 않아, 탈퇴가 **자기 손으로 되돌려졌다.**
"탈퇴시 토큰도 함께 만료" 요구는 이 구멍을 닫는 것이 본질이다.

## 어떻게 막는가 (issue #307 권장안)

토큰을 만료시킬 수 없으니 **유저 id를 내주지 않는다.**

```
BearerTokenAuthenticationFilter (JWT 검증 → SecurityContext)
  → CurrentUserIdArgumentResolver
      sub → UUID → UserService.isActive
      false면 AccountWithdrawnException
  → GlobalExceptionHandler → 401 WITHDRAWN_ACCOUNT
```

인증이 필요한 엔드포인트는 **전부 `@CurrentUserId`를 쓴다**(공개 엔드포인트만 안 쓴다). 그래서
리졸버 한 곳만 막으면 자동으로 전부 막힌다. `sub` 클레임이 `users.user_id`라는 계약
(decisions/016)에 기댄다.

### 필터 대신 리졸버를 고른 이유

`OncePerRequestFilter`로도 가능하지만 필터는 `GlobalExceptionHandler`를 타지 않아 에러 바디를
손으로 만들어야 한다. 리졸버에서 예외를 던지면 **표준 에러 응답 형식이 그대로 나온다.**
시큐리티 체인도 건드리지 않는다(failures/028 — 체인은 하나로 유지).

### 401인 이유

토큰 자체는 멀쩡하니 403(권한 없음)이 아니라 401이 맞느냐는 논쟁이 가능하다. **401로 정했다.**
FE 입장에서 "이 토큰으로는 더 못 쓴다 → 버리고 로그인 화면"이라는 처리가 토큰 만료와 동일하고,
기존 401 처리 로직을 그대로 태울 수 있기 때문이다. 대신 `code`를 `WITHDRAWN_ACCOUNT`로 따로 줘서
"갱신하면 되는 만료"와 "다시 들어올 수 없는 계정"을 구분할 수 있게 했다. 갱신을 시도해도 발급
단계에서 403으로 막힌다(decisions/048).

## Optional 변형은 막지 않는다

`@CurrentUserId Optional<UUID>`는 공개 엔드포인트에서 "로그인했으면 추가 정보"를 붙이는 데 쓴다
(`PlaceController` — 장소 상세의 찜 여부). 여기서 탈퇴 계정은 **차단하지 않고 `Optional.empty()`로
떨어뜨린다.** 토큰 상태와 무관하게 공개 API는 열려 있어야 한다는 게 팀 합의다(failures/028).
막아 버리면 탈퇴자가 비로그인 사용자보다 공개 API를 못 쓰는 이상한 상태가 된다.

## 판정은 fail-closed다 — 그래서 V38이 필요했다

`isActive`는 화이트리스트다. `ACTIVE`일 때만 통과하고, **유저 행이 없거나 `account_status`가
NULL이어도 false**다.

발급 단계에서는 안전한 선택이었지만, 매 요청 경로에서는 위험이 따라온다 —
`account_status`가 NULL인 행이 하나라도 있으면 **그 유저는 모든 API가 401**이 된다.
그래서 전제를 DB로 못 박았다.

**V38에서 `account_status`를 NOT NULL로 바꿨다**(NULL은 `ACTIVE`로 백필).
DEFAULT는 INSERT에서 컬럼을 생략했을 때만 적용돼 명시적 NULL을 막지 못한다.

## 비용

인증된 요청마다 `users` 조회가 한 번 늘어난다. 체감될 만큼 문제가 되면 짧은 TTL 캐시를 붙이되,
**탈퇴 반영이 TTL만큼 늦어진다**는 걸 감수할지 먼저 정하라.

## 테스트에 끼치는 영향 (일부러 감수함)

`WebMvcConfig`가 `UserService`를 필요로 하게 돼, `@WebMvcTest` 33개 전부 `UserService` 목이
필요하다. 그중 실제로 인증 요청을 보내는 28개는 `isActive`를 `true`로 스텁해야 한다
(화이트리스트라 Mockito 기본값 `false`가 곧 "탈퇴"이기 때문).

리졸버를 테스트 슬라이스 밖으로 뺐다면 이 손질을 피할 수 있었지만 그러지 않았다. failures/022가
정확히 그 실패다 — 테스트가 보지 못하는 시큐리티 동작은 운영에서 터진다. 인증된 요청이 전부
계정 상태를 거친다는 사실이 테스트에도 드러나는 편이 낫다.

## 남은 것 (이 결정의 범위 밖)

- **refresh_token은 탈퇴 시점에 폐기되지 않는다.** 쿠키 경로가 `/auth`라 `DELETE /users/me`
  요청에는 실려 오지 않아 값을 알 수 없다. 대신 다음 `POST /auth/refresh`에서 폐기되고 403이다
  (decisions/048). FE가 탈퇴 직후 `POST /auth/logout`을 부르면 즉시 폐기된다.
- **`PATCH /users/me`의 `accountStatus` 필드는 그대로 남아 있다.** 탈퇴한 계정이 도달할 수 없게
  됐으니 부활 경로는 닫혔지만, 유저가 자기 계정 상태를 임의로 쓰는 API 자체가 필요한지는 따로
  판단할 문제다.
- **chapchu-auth를 직접 호출하는 클라이언트는 이 차단에 걸리지 않는다.** 완전한 차단은
  발급 주체에서 해야 한다(decisions/008).

## 에이전트 행동 지침

- 계정 상태 판정은 `UserService.isActive`를 쓰고, 컨트롤러·리졸버에서 `account_status`를
  직접 비교하지 마라. 발급(048)과 요청(049)이 **같은 메서드**를 보게 유지하라.
- 시큐리티 체인을 두 개로 쪼개 해결하려 하지 마라(failures/028).
- 인증이 필요한 새 엔드포인트는 반드시 `@CurrentUserId`를 받아라. 안 받으면 이 차단을 비껴간다.
- 새 `@WebMvcTest`에는 `UserService` 목과 `isActive` → `true` 스텁을 함께 넣어라.
