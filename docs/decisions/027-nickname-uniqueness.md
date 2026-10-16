# 027. 닉네임 유일성 보장 및 중복 확인 API

## 상태

- [x] 확정됨 (Accepted)

## 배경

`users` 테이블에서 `email`과 `google_user_id`는 UNIQUE였지만 `nickname`에는 제약이 없어
여러 유저가 같은 닉네임을 가질 수 있었다. 로그인은 구글 OAuth로만 이루어지므로(decision 008),
**유저가 직접 정하는 식별자는 닉네임뿐**이고 화면에서 유저를 구분하는 값도 닉네임이다.

## 결정

### 1. 닉네임은 유저 간 중복될 수 없다 — 2단 방어

| 계층 | 수단 | 응답 |
|---|---|---|
| 애플리케이션 | `UserService`에서 `existsByNickname` 검사 | `409 Conflict` (`NicknameAlreadyInUseException`) |
| DB | `V6__add_users_nickname_unique.sql` (UNIQUE 인덱스) | 제약 위반 |

애플리케이션 검사만으로는 **동시 요청 경합(check-then-act)** 을 막을 수 없다.
두 요청이 동시에 "사용 가능"을 확인한 뒤 둘 다 저장하면 중복이 생긴다. DB 제약이 최종 방어선이다.

PostgreSQL은 UNIQUE 인덱스에서 NULL을 서로 다른 값으로 취급하므로,
아직 닉네임을 등록하지 않은 유저(`nickname IS NULL`)가 여럿 있어도 문제없다.

### 2. 본인 닉네임 재전송은 중복이 아니다

`PATCH /users/me`로 자기 닉네임을 그대로 보내는 경우(예: 다른 필드만 바꾸려고 전체 값을 보냄)는
중복으로 취급하지 않는다. 검사 전에 `nickname.equals(user.getNickname())`로 걸러낸다.

### 3. 중복 확인 API

```
GET /users/nickname/availability?nickname=초롱이
→ 200 {"nickname":"초롱이","available":false}
```

- **예약 효과가 없다.** 결과는 조회 시점 기준이며, 확인과 등록 사이에 다른 유저가 선점할 수 있다.
  클라이언트는 이 API를 UX용 사전 안내로만 쓰고, **최종 판정은 등록/수정 요청의 409 응답**으로 처리해야 한다.
- 인증이 필요하다(decision 025의 기본값). 구글 로그인 직후 온보딩 단계에서 호출되므로 토큰이 이미 있다.
  공개로 두면 닉네임 존재 여부를 무제한 조회할 수 있어 열거(enumeration)에 노출된다.
- 파라미터 검증(`@NotBlank`, `@Size(max = 30)`) 위반은 `400`이다.
  `@Validated` 파라미터 검증은 `ConstraintViolationException`을 던지는데 기존 핸들러가 없어 500이 나갔다.
  `GlobalExceptionHandler`에 핸들러를 추가해 `400 INVALID_REQUEST`로 매핑했다.

### 4. 대소문자·공백 정규화는 하지 않는다

현재는 **정확히 일치하는 문자열**만 중복으로 본다(`Choco`와 `choco`는 다른 닉네임).
한글 닉네임이 주 사용처라 대소문자 문제가 드물고, 정규화를 도입하면
`lower(nickname)` 함수 인덱스와 조회 로직을 함께 바꿔야 해 범위가 커진다.
필요해지면 별도 결정으로 다룬다.

## 에이전트 행동 지침

- 닉네임을 저장/변경하는 경로를 새로 만들면 반드시 `UserService`의 중복 검사를 태워라.
  DB 제약에만 의존하면 500이 나간다(제약 위반은 `DataIntegrityViolationException`).
- 중복 확인 API 결과를 신뢰해 등록 시 검사를 생략하지 마라. 경합 때문에 안전하지 않다.
- `@Validated` + 파라미터 제약을 쓰는 컨트롤러를 추가할 때는 400 매핑이 이미 되어 있으니 그대로 쓰면 된다.
