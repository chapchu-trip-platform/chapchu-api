# 026. 현재 유저 식별: @CurrentUserId ArgumentResolver

## 상태

- [x] 확정됨 (Accepted)
- decision 013(임시 인증 스텁)을 **완전히 대체**한다. `TempAuthContext`는 제거되었다.
- decision 016의 "에이전트 행동 지침 2번"(각 Controller에서 `@AuthenticationPrincipal Jwt`를 직접 받기)을 대체한다.

## 배경

decision 025로 JWT 검증(인증 강제)까지는 적용했지만, 컨트롤러들은 여전히
`TempAuthContext.TEMP_USER_ID`라는 고정 상수를 현재 유저로 사용하고 있었다.
그 결과 배포 환경에서 유저 컨텍스트가 필요한 엔드포인트가 전부 404였다(issue #47).

```
GET /home                 → 404 {"code":"NOT_FOUND","message":"유저를 찾을 수 없습니다."}
GET /users/me             → 404 (동일)
GET /users/me/preferences → 404 (동일)
```

운영 DB에 `11111111-...` 가짜 유저 행을 넣는 것은 해결책이 아니다(decision 013에 명시).
JWT에서 실제 유저를 식별하는 것이 유일한 해법이다.

## 결정

`@CurrentUserId UUID` 파라미터로 현재 유저를 주입받는다.

```java
@GetMapping
public List<PetResponse> listPets(@CurrentUserId UUID userId) {
  return petService.listPets(userId).stream().map(PetResponse::from).toList();
}
```

| 구성 요소 | 위치 | 역할 |
|---|---|---|
| `@CurrentUserId` | `module-common` / `com.pettrip.common.service` | 순수 Java 애노테이션. 의존성 없음 |
| `CurrentUserIdArgumentResolver` | `app` / `com.pettrip.config` | JWT `sub` → `UUID` 추출 |
| `WebMvcConfig` | `app` / `com.pettrip.config` | 리졸버 등록 |
| `UnauthenticatedRequestException` | `app` / `com.pettrip.config` | `AuthenticationException` 계열 → 401 변환 |

`sub` 클레임이 `users.user_id`(UUID v7 문자열)라는 decision 016 계약에 의존한다.

## 이유: 왜 `@AuthenticationPrincipal Jwt`를 직접 쓰지 않았나

decision 016은 각 컨트롤러에서 `@AuthenticationPrincipal Jwt jwt`를 받아
`UUID.fromString(jwt.getSubject())`를 호출하라고 안내했지만, 실제 적용해 보니 두 가지 문제가 있었다.

1. **모듈 의존성 오염** — `Jwt`/`@AuthenticationPrincipal`은 Spring Security 타입이다.
   그대로 따르면 `module-user`, `module-community`, `module-review`에 Spring Security 의존성을 추가해야 한다.
   도메인 모듈은 현재 web/jpa/validation만 의존하고 있어, 인증 기술을 도메인 모듈까지 퍼뜨리지 않는 편이 낫다.
2. **추출 로직 중복** — `UUID.fromString(jwt.getSubject())`가 컨트롤러 핸들러 약 20곳에 복제된다.
   `sub` 형식이 바뀌거나 검증을 추가할 때 20곳을 고쳐야 한다.

리졸버 방식은 추출을 한 곳에 모으고, 도메인 모듈은 순수 애노테이션만 참조하므로 의존성이 늘지 않는다.
컨트롤러 시그니처에 `UUID userId`가 드러나 테스트에서 유저를 바꿔 끼우기도 쉽다.

## 테스트

- 컨트롤러 테스트는 `@AutoConfigureMockMvc(addFilters = false)`를 **제거**하고 실제 보안 필터를 사용한다.
  `@Import(SecurityConfig.class)` + `@MockitoBean JwtDecoder` + 요청에
  `.with(jwt().jwt(j -> j.subject(USER_ID.toString())))`를 붙인다.
  - `jwt()` 후처리기는 `SecurityContextHolder`를 직접 채우지 않고 요청에만 저장하므로,
    **필터를 끄면 리졸버가 인증 정보를 찾지 못한다.** 그래서 필터를 켜야 한다(docs/failures/019 참고).
  - 부수 효과로 모든 컨트롤러 테스트가 보안 설정까지 함께 검증하게 되었다.
- `sub`가 실제로 서비스까지 전달되는지는 `PetControllerTest`의
  `JWT_sub_클레임의_유저_ID가_서비스로_전달된다`에서 `verify`로 확인한다.
  다른 테스트들은 userId를 `any()`로 받으므로 이 배선을 검증하지 못한다.
- REST Docs 스니펫은 영향받지 않는다. `jwt()`는 `Authorization` 헤더를 추가하지 않고 인증 객체를 직접 주입한다.

## 미해결 (별도 결정 필요)

- **유저 자동 프로비저닝 여부** — 유효한 JWT를 들고 왔지만 `users` 행이 없으면 현재는 404다.
  chapchu-auth가 `users` 테이블을 직접 공유 매핑하므로(decision 016) 로그인 시점에 행이 생성될 것으로 보이지만,
  chapchu-auth 쪽 구현 확인 후 "api에서 자동 생성" vs "404 유지"를 결정해야 한다.

## 에이전트 행동 지침

- 유저 컨텍스트가 필요한 새 핸들러는 `@CurrentUserId UUID userId` 파라미터를 받아라.
  `SecurityContextHolder`를 컨트롤러/서비스에서 직접 조회하지 마라.
- 도메인 모듈(`module-*`)에 Spring Security 의존성을 추가하지 마라. 인증 관련 코드는 `app`의 `com.pettrip.config`에 둔다.
- 새 컨트롤러 테스트는 `addFilters = false`를 쓰지 말고 위 "테스트" 절의 조합을 따라라.
