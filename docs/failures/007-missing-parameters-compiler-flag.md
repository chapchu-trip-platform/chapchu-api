# 007. `-parameters` 컴파일러 플래그 누락으로 `@PathVariable` 이름 인식 실패

## 증상
`@PathVariable UUID petId`처럼 이름을 명시하지 않은 경로 변수를 쓰면 `@WebMvcTest` MockMvc 요청에서:
```
IllegalArgumentException: Name for argument of type [java.util.UUID] not specified,
and parameter name information not available via reflection.
Ensure that the compiler uses the '-parameters' flag.
```

## 실패 원인
- `build.gradle`에 `-parameters` 컴파일러 옵션이 없어서, 컴파일된 클래스에 파라미터 이름 메타데이터가 남지 않음.
- Spring MVC는 리플렉션으로 파라미터 이름을 읽어 `@PathVariable`/`@RequestParam` 이름을 추론하는데, 이 메타데이터가 없으면 실패.

## 해결책 (현재 적용)
루트 `build.gradle`의 `subprojects` 블록에 아래 추가:
```gradle
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs << '-parameters'
}
```

## 에이전트 행동 지침
- 이 컴파일러 옵션을 제거하지 마라. 제거하면 이름 없는 `@PathVariable`/`@RequestParam`이 전부 깨진다.
- `@PathVariable("petId")`처럼 매번 이름을 명시하는 우회는 하지 마라 — 전역 설정으로 해결된 문제이므로 불필요한 보일러플레이트다.
