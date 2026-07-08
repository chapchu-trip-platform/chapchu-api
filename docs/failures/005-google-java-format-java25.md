# 005. google-java-format 1.17.0 — Java 25 컴파일러 내부 API 비호환

## 증상
Java 25 툴체인(JAVA_HOME)에서 `./gradlew spotlessApply` 실행 시 `:app:spotlessJava` 태스크가 아래 오류로 실패:
```
'java.util.Queue com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()'
```

## 실패 원인
- Java 25에서 `com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()`의 반환 타입이 `Queue<JCDiagnostic>`에서 `List<JCDiagnostic>`으로 변경됨 (바이너리 비호환).
- google-java-format 1.17.0(spotless 6.25.0 기본 연동 버전)은 javac 내부 API를 리플렉션 없이 직접 참조하므로, Gradle 데몬을 Java 25로 구동하면 `NoSuchMethodError` 발생.
- `docs/failures/001-spotbugs-java25-asm.md`와 동일한 종류(내부 API 의존 도구가 새 Java 버전에서 깨지는 패턴).

## 해결책 (현재 적용)
- `build.gradle`의 `spotless { java { googleJavaFormat('1.17.0') } }` → `googleJavaFormat('1.35.0')`으로 상향.
- 1.34.0 이상부터 Java 25 관련 대응이 반영되어 있어 정상 동작 확인 (`spotlessApply`, `check` 모두 BUILD SUCCESSFUL).

## 에이전트 행동 지침
- `googleJavaFormat` 버전을 1.17.0 등 구버전으로 되돌리지 마라.
- google-java-format/spotless 관련 빌드 실패 시, 먼저 Java 버전과 내부 컴파일러 API 비호환 가능성을 의심하고 최신 버전 업그레이드부터 시도하라.
- Spotless Gradle 플러그인 자체(`com.diffplug.spotless` 6.25.0)도 향후 Java 26+ 대응 문제가 생기면 최신 버전 확인 후 상향하라.

## 관련 문서
- 동일 증상에 대한 대안(버전 다운그레이드 + JVM `--add-exports`/`--add-opens`) 시도 기록: [[011-spotless-googlejavaformat-java25-jvm-args]]. 현재 `build.gradle`은 본 문서의 1.35.0 상향안이 적용되어 있다.
