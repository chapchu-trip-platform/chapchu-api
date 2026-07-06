# 001. SpotBugs 4.9.3 + Java 25 클래스 파일 파싱 실패

## 실패 원인
- SpotBugs 4.9.3 이 번들하는 ASM 9.7.1 은 Java 25 클래스 파일(major version 69) 을 지원하지 않는다.
- `build.gradle` 에서 `toolVersion = '4.9.3'` 만 설정하면 여전히 ASM 9.7.1 이 사용되어 빌드 실패.
- 오류 메시지: `IllegalArgumentException: Unsupported class file major version 69`

## 해결책 (현재 적용)
- `configurations.named('spotbugs')` 블록에서 ASM을 9.8 로 강제 교체.
- SpotBugs 공식 릴리즈가 ASM 9.8+ 를 번들하는 버전이 나오면 해당 우회 코드를 제거한다.

## 에이전트 행동 지침
- `build.gradle` 의 ASM 강제 교체 블록(`resolutionStrategy`)을 임의로 삭제하지 마라.
- SpotBugs `toolVersion` 을 올릴 때는 해당 버전의 번들 ASM 버전을 반드시 확인하라.
