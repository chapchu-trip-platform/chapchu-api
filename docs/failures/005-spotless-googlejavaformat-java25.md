# 005: Spotless googleJavaFormat 1.17.0 + Java 25 호환성 오류

## 증상
```
Execution failed for task ':app:spotlessJava'.
> 'java.util.Queue com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()'
```

## 원인
`googleJavaFormat 1.17.0`이 Java 25에서 제거된 내부 javac API
(`com.sun.tools.javac.util.Log$DeferredDiagnosticHandler.getDiagnostics()`)를 사용함.

## 해결
1. `build.gradle`: `googleJavaFormat('1.17.0')` → `googleJavaFormat('1.27.0')`
2. `gradle.properties` 생성: Java 25용 `--add-exports` / `--add-opens` JVM args 추가

```properties
org.gradle.jvmargs=\
  --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
  --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  ...
```
