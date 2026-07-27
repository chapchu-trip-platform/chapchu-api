# 010. `@DataJpaTest`는 `JpaConfig`(`@EnableJpaAuditing`)를 자동으로 로드하지 않는다

## 증상
`@DataJpaTest` 슬라이스 테스트에서 저장 직후 `entity.getCreatedAt()`/`getUpdatedAt()`을 조회하면 `null`이 반환된다. 실제 운영 애플리케이션(전체 `@SpringBootApplication` 컨텍스트)에서는 정상 동작한다.

## 실패 원인
- `@EnableJpaAuditing`은 `module-common`의 `com.pettrip.common.config.JpaConfig`에 선언되어 있다.
- `@DataJpaTest`는 컴포넌트 스캔 범위를 JPA 관련 빈(Entity, Spring Data Repository 등)으로 제한하고, 일반 `@Configuration` 클래스는 테스트 컨텍스트에 포함하지 않는다.
- 그 결과 `AuditingEntityListener`가 등록은 되어도 실제 `AuditingHandler`가 없어 `@CreatedDate`/`@LastModifiedDate`가 조용히 무시된다 (예외 없이 `null`).
- 기존 `PetRepositoryTest`/`UserRepositoryTest`는 `createdAt`/`updatedAt`을 검증하지 않아서 지금까지 발견되지 않았다.

## 해결책 (적용 완료)
`createdAt`/`updatedAt` 값을 검증하는 `@DataJpaTest`에는 반드시 `@Import(JpaConfig.class)`를 추가한다:
```java
@DataJpaTest
@Import(JpaConfig.class)
class PhotoRepositoryTest { ... }
```
(`app/src/test/java/com/pettrip/photo/repository/PhotoRepositoryTest.java` 참고)

## 에이전트 행동 지침
- 새 `@DataJpaTest`에서 `createdAt`/`updatedAt`(또는 향후 `createdBy`/`lastModifiedBy`)을 검증해야 한다면 `@Import(com.pettrip.common.config.JpaConfig.class)`를 함께 붙여라.
- 검증하지 않는 기존 `@DataJpaTest`(`PetRepositoryTest`, `UserRepositoryTest` 등)는 지금 당장 고칠 필요 없음 — 실제로 값이 필요해질 때 이 문서를 참고해 추가하라.
- 이 문제는 테스트 슬라이스에서만 발생한다. 운영 애플리케이션은 `@SpringBootApplication` 전체 스캔으로 `JpaConfig`를 정상적으로 로드하므로 실제 서비스 동작에는 영향이 없다.
