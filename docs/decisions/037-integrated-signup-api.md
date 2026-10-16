# 037. 통합 회원가입 API — POST /auth/signup

## 배경

기존 온보딩 흐름은 여러 단계였다.

```
POST /auth/register  →  (재로그인)  →  POST /users/me/preferences  →  POST /pets  →  POST /pets  →  ...
```

문제:
1. **부분 실패**: 중간 단계가 실패하면 chapchu-auth에는 유저가 생성됐지만 chapchu-api DB에는 없거나,
   반대로 chapchu-api에만 있는 불완전한 계정이 남았다.
2. **재시도 불가**: `POST /auth/register`가 chapchu-auth에 유저를 만들어버리기 때문에 실패 후
   같은 `registration_token`으로 재시도하면 "이미 존재하는 유저" 오류가 났다.
3. **API 호출 수**: 반려동물 2마리를 등록하려면 최소 5번 호출해야 했다.

## 결정

`POST /auth/signup` 엔드포인트를 추가한다.

- 요청 한 번에 nickname, 선호 사항(지역/테마/이동수단), 반려동물 목록을 받는다.
- chapchu-auth의 `/auth/registration-token/verify`로 `registration_token`의 유효성만 검증한다.
  chapchu-auth에 유저를 **생성하지 않는다.**
- chapchu-api DB에 유저·선호사항·반려동물을 **단일 트랜잭션**으로 저장한다.
  실패하면 아무것도 남지 않아 같은 `registration_token`으로 재시도할 수 있다.

### 왜 chapchu-auth의 /auth/register를 부르지 않는가

```
// RegistrationTokenClient.java 주석에서
chapchu-auth의 POST /auth/register를 대신 부르면 유저가 이미 만들어져 재시도가 깨진다.
```

verify는 읽기 전용이라 트랜잭션이 실패해도 chapchu-auth에 흔적이 남지 않는다.

### /auth/register 제거

통합 회원가입 이후 기존 `POST /auth/register`(chapchu-auth 프록시)는 불필요해졌다.
`POST /auth/signup`이 완전히 대체하므로 `AuthController.register()` 메서드와
`SecurityConfig.PUBLIC_PATHS`의 `/auth/register` 항목을 삭제했다(038 참고).

## SignupRequest 구조

```json
{
  "registrationToken": "...",
  "user": {
    "nickname": "초롱이아빠",
    "regionIds": ["uuid..."],
    "themeIds": ["uuid..."],
    "transportMethodIds": ["uuid..."]
  },
  "pets": [
    { "petName": "초롱이", "breedId": 42, "size": "MEDIUM", "age": 3, "activityIds": ["uuid..."] }
  ]
}
```

## 에이전트 행동 지침

- 온보딩 흐름은 반드시 `/auth/signup` 단일 엔드포인트로 완결해야 한다.
- `POST /auth/register`는 삭제됐다. 다시 추가하지 마라.
- 회원가입 중 하나라도 실패하면 전체가 롤백돼야 한다. 서비스 메서드에 `@Transactional` 필수.
- `registration_token` 검증은 `RegistrationTokenClient.verify()`만 호출한다.
  chapchu-auth의 `/auth/register`를 추가로 호출하면 재시도가 깨진다.
