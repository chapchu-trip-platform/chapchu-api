# 001. Java 25 LTS 채택

## 상태
- [x] 확정됨 (Accepted)

## 결정
- 런타임 및 컴파일 타겟으로 Java 25 (LTS) 를 사용한다.
- Gradle toolchain: `JavaLanguageVersion.of(25)`, Temurin 배포판.

## 에이전트 행동 지침
- 새 Java 문법/API 사용 시 Java 25 이하 버전 호환성을 고려하지 않아도 된다.
- `build.gradle` toolchain 버전을 임의로 낮추지 마라.
