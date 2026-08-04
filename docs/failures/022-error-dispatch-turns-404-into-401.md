# 022. ERROR 디스패치를 permitAll 하지 않아 모든 404가 401로 둔갑

## 증상

공개 경로인데도 404가 아니라 401이 반환된다.

```
GET /docs/index.html        200   ← permitAll, 파일 있음
GET /docs/nonexistent.html  401   ← permitAll인데 401 (404여야 함)
GET /places                 401   ← PUBLIC_GET_PATHS인데 401
```

프론트는 "경로가 없다"와 "인증이 필요하다"를 구분할 수 없다.
실제로 `/places` 미배포(이슈 #50)를 진단할 때 이 401 때문에 원인 파악이 늦어졌다.

## 원인

404가 나면 서블릿 컨테이너가 `/error`로 **forward** 한다. 이 ERROR 디스패치도 시큐리티 필터 체인을 다시 탄다
(Spring Security 6부터 `shouldFilterAllDispatcherTypes` 기본값이 true).

`/error`는 `PUBLIC_PATHS` 어디에도 없으므로 `anyRequest().authenticated()`에 걸려 401이 된다.

```
GET /docs/nonexistent.html
  └ REQUEST 디스패치 → permitAll 통과 → 핸들러 없음 → 404
      └ ERROR 디스패치로 /error forward → 인증 필요 → 401  ← 최종 응답
```

## 테스트가 잡지 못한 이유

`SecurityConfigTest`에 이미 이런 테스트가 있었고 **통과하고 있었다.**

```java
mockMvc.perform(get("/places/test-place-id")).andExpect(status().isNotFound());
```

MockMvc는 실제 서블릿 컨테이너와 달리 **에러 페이지 forward를 일으키지 않는다.**
그래서 404에서 멈추고 ERROR 디스패치가 발생하지 않아 버그가 재현되지 않았다.
테스트는 초록불인데 운영은 깨져 있는 전형적인 거짓 안심이다.

## 해결

```java
.authorizeHttpRequests(auth ->
    auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
        .permitAll()
        .requestMatchers(PUBLIC_PATHS).permitAll()
        ...)
```

에러 응답 본문은 `{"code","message"}` 형태뿐이라 공개해도 노출되는 정보가 없다.

검증 테스트는 디스패치 타입을 직접 지정해 MockMvc의 한계를 우회한다.

```java
mockMvc.perform(get("/error").with(request -> {
      request.setDispatcherType(DispatcherType.ERROR);
      return request;
    }))
    .andExpect(status().is(not(401)));
```

규칙을 제거하면 이 테스트가 실패하는 것을 확인했다.

## 에이전트 행동 지침

- 시큐리티 설정을 검증할 때 **MockMvc가 재현하지 못하는 동작이 있다**는 점을 전제하라.
  에러 페이지 forward, 비동기 디스패치, 서블릿 컨테이너 고유 동작이 그렇다.
- 슬라이스 테스트가 통과했다고 운영 동작을 단정하지 마라. 배포 후 실제 HTTP 상태 코드를 확인하라.
- 공개 경로인데 401이 나오면 경로 매칭이 아니라 **에러 디스패치**를 먼저 의심하라.
