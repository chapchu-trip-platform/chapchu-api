# 013. 임시 인증 스텁 (실제 OAuth2 Resource Server 붙기 전)

## 상태
- [x] **폐기됨 (Superseded)** — `SecurityConfig` 항목은 decision 025, `TempAuthContext` 항목은 decision 026이 대체함
- 이 문서는 히스토리 보존용이다. **아래 내용을 새 코드에 적용하지 마라.**

## 폐기 요약

| 원래 결정 | 대체 결정 | 현재 상태 |
|---|---|---|
| `SecurityConfig`에서 모든 요청 `permitAll()` | decision 025 | 실제 JWT 검증 + 공개/인증 엔드포인트 분리 |
| 컨트롤러가 `TempAuthContext.TEMP_USER_ID` 사용 | decision 026 | `@CurrentUserId UUID` 리졸버로 JWT `sub`에서 추출. `TempAuthContext` 클래스 삭제됨 |

`TEMP_USER_ID`(`11111111-1111-1111-1111-111111111111`) 가짜 유저 행은 더 이상 필요하지 않다.
로컬 DB에 만들어 둔 행이 있다면 지워도 된다.

---

## (히스토리) 원래 결정
- `auth`/`user` 도메인의 실제 JWT 인증(decision 008, chapchu-auth 연동)이 구현되기 전까지:
  - ~~`app/src/main/java/com/pettrip/config/SecurityConfig.java`에 모든 요청을 `permitAll()` 처리하는 임시 `SecurityFilterChain`을 둔다.~~
  - ~~인증이 필요한 Controller는 `@AuthenticationPrincipal` 대신 `com.pettrip.common.service.TempAuthContext.TEMP_USER_ID`(모든 도메인이 공유하는 고정 상수)를 현재 유저로 사용한다.~~

## (히스토리) 이유
- pet 등 유저 소유 리소스 도메인을 auth 도메인보다 먼저 구현하게 되어, 실제 인증 없이도 소유권 로직/TDD/REST Docs 패턴을 검증할 수 있어야 했음.

## (히스토리) 이 스텁이 남긴 문제
- 배포 환경에서 `users` 테이블에 `TEMP_USER_ID` 행이 없어 `GET /home`, `GET /users/me`,
  `GET /users/me/preferences`가 전부 404였다(issue #47). 가짜 행을 운영 DB에 넣는 것은 해결책이 아니므로
  decision 026으로 실제 JWT 유저 식별을 도입해 해소했다.
