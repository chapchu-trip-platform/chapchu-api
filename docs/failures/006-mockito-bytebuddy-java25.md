# 006. Mockito(byte-buddy) 5.14.2 — Java 25 클래스 파일 비호환 + Spring BOM 강제 다운그레이드

## 증상
Java 25 툴체인에서 `@Mock`을 사용하는 테스트(Mockito) 실행 시:
```
org.mockito.exceptions.base.MockitoException
  Caused by: java.lang.IllegalStateException at InlineBytecodeGenerator
    Caused by: java.lang.IllegalArgumentException at OpenedClassReader.java:120
```

## 실패 원인
- Spring Boot 3.4.5 BOM이 관리하는 `mockito-core` 5.14.2는 byte-buddy 1.15.11을 사용하는데, byte-buddy가 Java 25 클래스 파일(major version 69)을 지원하기 시작한 건 1.17.5부터임.
- `mockito-core`만 5.21.0으로 올려도, Spring BOM이 `net.bytebuddy:byte-buddy`를 1.15.11로 강제 고정하고 있어서 mockito가 요구하는 1.17.7을 Gradle이 다시 1.15.11로 다운그레이드함 (`docs/failures/001`, `005`와 동일한 부류의 Java 25 생태계 문제).

## 해결책 (현재 적용)
`app/build.gradle` testImplementation에 아래 4개를 명시적으로 버전 고정:
```gradle
testImplementation 'org.mockito:mockito-core:5.21.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.21.0'
testImplementation 'net.bytebuddy:byte-buddy:1.17.7'
testImplementation 'net.bytebuddy:byte-buddy-agent:1.17.7'
```
mockito-core만 올리는 것으로는 부족하고, byte-buddy/byte-buddy-agent도 반드시 함께 명시적으로 올려야 Spring BOM의 강제 고정을 이긴다.

## 에이전트 행동 지침
- Mockito 관련 테스트가 Java 25에서 `OpenedClassReader`/`InlineBytecodeGenerator` 예외로 실패하면, mockito-core뿐 아니라 byte-buddy/byte-buddy-agent 버전도 함께 명시적으로 올려라.
- Spring Boot BOM이 관리하는 라이브러리를 상향할 때는 `./gradlew :app:dependencies --configuration testRuntimeClasspath`로 실제 해석된 버전을 반드시 확인하라 (요청 버전과 최종 해석 버전이 다를 수 있음).
- 위 4개 버전 고정 라인을 임의로 제거하거나 낮추지 마라.
