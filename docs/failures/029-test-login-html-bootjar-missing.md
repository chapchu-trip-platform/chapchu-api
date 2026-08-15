---
title: "029 - test-login.html이 bootJar에 포함되지 않아 배포 후 404"
date: 2026-07
status: resolved
---

## 현상

`app/src/main/resources/static/docs/test-login.html`을 추가하고 배포했는데
`https://api.chapchu.site/docs/test-login.html`이 404를 반환했다.

로컬 `bootRun`에서는 정상 서빙됨.

## 원인 1 — Gradle 멀티모듈 의존성 경로

Gradle 멀티모듈 구조에서 `app` 모듈이 `module-*`를 의존할 때,
`bootJar` 태스크가 `resources/static/` 파일을 패키징하는 과정에서
다른 모듈의 정적 파일과 경로가 충돌하면 `DuplicatesStrategy` 오류로 조용히 누락됐다.

```
> Entry resources/static/docs/... from ... and ... have duplicate paths
```

## 원인 2 — asciidoctor 스니펫 경로 중복

REST Docs 빌드 시 `asciidoctor` 태스크가 스니펫 경로를 상대경로로 설정했고,
Gradle이 이를 두 번 적용해 실제 경로가 `build/generated-snippets/generated-snippets/...`가 됐다.
HTML이 생성돼도 올바른 위치에 없어 bootJar에 포함되지 않았다.

## 해결

```groovy
// build.gradle
bootJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// asciidoctor 경로를 절대경로로 변경
asciidoctor {
    sourceDir = file("src/docs/asciidoc")
    outputDir = file("${buildDir}/docs/asciidoc")
    ...
}
```

CI에서 `test` 태스크를 실행해 REST Docs 스니펫이 생성된 후 `bootJar`가 실행되도록
Gradle 태스크 의존성 순서를 명시했다.

## 재발 방지

- `app` 모듈의 정적 파일을 추가할 때 `bootJar` 태스크 실행 후
  `jar -tf build/libs/*.jar | grep static` 으로 포함 여부를 확인하라.
- asciidoctor 경로는 항상 **절대경로**로 지정하라. 상대경로는 Gradle 컨텍스트에 따라 이중 적용된다.
- CI에서 `test` → `asciidoctor` → `bootJar` 순서가 보장되는지 Gradle 태스크 그래프를 확인하라.
