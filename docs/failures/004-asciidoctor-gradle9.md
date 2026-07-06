# 004. Asciidoctor Gradle 플러그인 3.3.2 — Gradle 9 미지원

## 증상
`org.asciidoctor.jvm.convert:3.3.2` 적용 시:
```
Could not create an instance of type org.asciidoctor.gradle.jvm.AsciidoctorJExtension.
  > org/gradle/util/CollectionUtils
```

## 원인
Gradle 9에서 `org.gradle.util.CollectionUtils`가 제거됨.
Asciidoctor Gradle 플러그인 3.x 계열이 해당 API에 의존.

## 해결
- 플러그인 제거, `spring-restdocs-mockmvc`로 스니펫만 생성
- HTML 렌더링: CI 파이프라인에서 `asciidoctor` CLI 직접 실행하거나
  Gradle 9 호환 버전(4.x 이상) 확인 후 재도입

## 에이전트 행동 지침
- Gradle 9 환경에서 `org.asciidoctor.jvm.convert` 3.x 계열을 추가하지 마라.
- REST Docs 스니펫 생성은 `spring-restdocs-mockmvc` 의존성만으로 충분하다.
