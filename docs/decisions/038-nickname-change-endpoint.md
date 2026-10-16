# 038. POST /auth/register 제거 및 닉네임 변경 엔드포인트 도입

## 배경

`POST /auth/signup`(037)이 온보딩 흐름을 완전히 대체하면서 두 엔드포인트가 중복·불필요해졌다.

1. **`POST /auth/register`**: chapchu-auth의 `/auth/register`로 요청을 프록시하던 엔드포인트.
   통합 회원가입 이후 아무도 호출하지 않는 죽은 코드가 됐다.

2. **`POST /users/me`** (`registerNickname`): 닉네임이 없는 유저에게 **최초 1회**만 허용하는 등록 엔드포인트.
   `UserService.registerNickname()`에 `if (user.hasNickname()) throw NicknameAlreadyRegisteredException()`
   제약이 있었다. 통합 회원가입 후에는 모든 유저가 가입 시 닉네임을 갖게 되므로 이 엔드포인트는
   쓸 일이 없어졌다. 하지만 닉네임 **변경** 기능은 여전히 필요하다.

## 결정

1. `AuthController.register()` 메서드 삭제, `SecurityConfig.PUBLIC_PATHS`에서 `/auth/register` 제거.
2. `POST /users/me` → 삭제.
3. `PATCH /users/me/nickname` 신설: 닉네임 **변경** 전용 엔드포인트. 횟수 제한 없음.
   내부적으로 기존 `UserService.updateMe(userId, nickname, null)` 로직 그대로 사용한다.
4. `UserService.registerNickname()` 삭제.
5. `NicknameAlreadyRegisteredException` 삭제 (사용처 없음).
6. `NicknameRegisterRequest` → `NicknameChangeRequest` 교체.

### 변경된 엔드포인트

| 이전 | 이후 | 인증 |
|---|---|---|
| `POST /auth/register` (chapchu-auth 프록시) | 제거 | — |
| `POST /users/me` (닉네임 최초 등록, 1회 제한) | 제거 | — |
| — | `PATCH /users/me/nickname` (닉네임 변경) | access_token |

## 에이전트 행동 지침

- `POST /auth/register`와 `POST /users/me`(registerNickname)는 삭제됐다. 다시 추가하지 마라.
- 닉네임 변경은 `PATCH /users/me/nickname`이다. `PATCH /users/me`는 닉네임+accountStatus를 함께 변경하는
  더 넓은 엔드포인트다. 용도에 맞게 구분해 사용하라.
- `UserService.registerNickname()`은 없다. 코드에서 참조하지 마라.
