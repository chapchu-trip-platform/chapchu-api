# 003. Checkstyle ImportControl — first match wins (last match wins 아님)

## 증상
`<disallow pkg=".*" regex="true"/>`를 먼저 선언하고 그 뒤에 `<allow pkg="org.springframework"/>` 선언 시,
Spring 임포트가 여전히 차단됨.

## 원인
Checkstyle ImportControl은 **first match wins** 방식이다.
`disallow .*` 가 먼저 매칭되므로 이후 allow 규칙은 도달하지 않는다.

## 해결
allow 규칙을 disallow 앞에 선언한다.

```xml
<!-- 올바른 순서 -->
<allow pkg="java"/>
<allow pkg="org.springframework"/>
<allow pkg="com.pettrip"/>
<disallow pkg=".*" regex="true"/>  ← catch-all은 마지막
```

## 에이전트 행동 지침
- `import-control.xml` 수정 시 allow를 항상 disallow 앞에 위치시켜라.
- 신규 외부 라이브러리 추가 시 `import-control.xml`에 해당 패키지 allow를 먼저 추가하라.
