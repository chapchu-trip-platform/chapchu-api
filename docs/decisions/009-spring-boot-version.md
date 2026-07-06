# 009. Spring Boot 버전 및 주요 의존성 버전

## 상태
- [x] 확정됨 (Accepted)

## 결정
| 라이브러리 | 버전 | 비고 |
|---|---|---|
| Spring Boot | 3.4.5 | LTS 안정 버전 |
| Java | 25 | LTS |
| Gradle | 9.0.0 | |
| Spring AI BOM | 미확정 | RAG 구현 시 최신 GA 버전 확인 후 적용 |
| Spring Cloud AWS | 3.2.0 | S3 연동 |
| uuid-creator | 5.3.7 | UUID v7 생성 |
| AWS SDK BOM | 2.26.0 | |

## Spring AI 적용 지연 이유
- `spring-ai-bom:1.0.0` Maven Central 해석 실패 확인
- `module-recommendation` 소스 미구현 상태이므로 RAG 구현 착수 전 버전 재확인 예정
- `module-recommendation/build.gradle`에 주석으로 보존

## 에이전트 행동 지침
- Spring AI 의존성 활성화 전 반드시 Maven Central에서 최신 GA 버전을 확인하라.
- Spring Boot 버전 업그레이드 시 Spring AI, Spring Cloud AWS 호환성을 함께 확인하라.
