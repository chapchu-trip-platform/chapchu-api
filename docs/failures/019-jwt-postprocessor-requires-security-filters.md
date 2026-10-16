# 019. `jwt()` 요청 후처리기는 `addFilters = false`와 함께 쓸 수 없다

## 증상

`@CurrentUserId` 리졸버(decision 026) 도입 후, 기존 컨트롤러 테스트에
`.with(jwt().jwt(j -> j.subject(...)))`만 추가했는데도 전부 실패했다.

```
jakarta.servlet.ServletException
  Caused by: com.pettrip.config.UnauthenticatedRequestException
```

## 원인

기존 테스트는 `@AutoConfigureMockMvc(addFilters = false)`로 보안 필터를 껐다.

`SecurityMockMvcRequestPostProcessors.jwt()`는 인증 객체를 **요청(request)에 저장**할 뿐,
`SecurityContextHolder`를 직접 채우지 않는다. 요청에 저장된 컨텍스트를 홀더로 옮기는 일은
`SecurityContextHolderFilter`가 하는데, 필터를 껐으니 그 단계가 실행되지 않는다.

결과적으로 리졸버가 `SecurityContextHolder.getContext().getAuthentication()`에서 `null`을 받아 예외를 던졌다.

## 해결

컨트롤러 테스트에서 보안 필터를 켠다.

```java
@WebMvcTest(PetController.class)
@Import(SecurityConfig.class)          // 실제 보안 설정 사용
class PetControllerTest {
  @MockitoBean private JwtDecoder jwtDecoder;   // issuer-uri 없이 슬라이스 기동하려면 필요

  // 요청마다
  mockMvc.perform(get("/pets").with(jwt().jwt(j -> j.subject(USER_ID.toString()))))
}
```

`@AutoConfigureMockMvc(addFilters = false)`는 제거한다.

## 에이전트 행동 지침

- 인증된 유저가 필요한 컨트롤러 테스트에 `addFilters = false`를 쓰지 마라.
  필터를 끄면 `jwt()`/`authentication()` 후처리기가 무력화된다.
- `oauth2ResourceServer(jwt)` 설정을 `@Import`하는 슬라이스 테스트는 `JwtDecoder`를 목으로 주입해야 한다.
  실제 `JwtDecoder`는 `issuer-uri`(`AUTH_SERVER_URL`)로 디스커버리를 시도하므로 테스트에서 기동할 수 없다.
