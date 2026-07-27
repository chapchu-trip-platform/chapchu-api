# 011: Spotless googleJavaFormat 1.17.0 + Java 25 호환성 오류 (JVM args 접근)

## 증상
```
Execution failed for task ':app:spotlessJava'.
> 'java.util.Queue com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()'
```

## 원인
`googleJavaFormat 1.17.0`이 Java 25에서 제거된 내부 javac API
(`com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()`)를 사용함.

## 시도한 해결책
1. `build.gradle`: `googleJavaFormat('1.17.0')` → `googleJavaFormat('1.27.0')`
2. `gradle.properties` 생성: Java 25용 `--add-exports` / `--add-opens` JVM args 추가

```properties
org.gradle.jvmargs=\
  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  ...
```

## 참고: 최종 적용된 해결책은 다름
동일 증상에 대해 [[005-google-java-format-java25]]에서 독립적으로 `googleJavaFormat('1.35.0')` 상향만으로 해결했고, 현재 `build.gradle`에는 이 버전이 반영되어 있다.
`--stop` 후 데몬을 새로 띄워 `gradle.properties`의 JVM args 없이 `spotlessApply`를 실행해도 1.35.0에서는 정상 동작함을 확인함 (2026-07-07 검증).
즉 이 문서의 JVM args는 1.35.0 기준으로는 더 이상 필수가 아니며, 현재는 안전망으로만 유지된다. 두 문서 다 남기는 이유는 같은 실패에 대한 두 가지 접근을 모두 기록해 두기 위함.
