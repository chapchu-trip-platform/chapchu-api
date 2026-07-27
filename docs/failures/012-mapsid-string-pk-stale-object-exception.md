# 012. @MapsId + 수동 할당 String PK → StaleObjectStateException

## 증상
`@MapsId`로 부모 엔티티의 PK를 공유하는 자식 엔티티를 `repository.save()`로 저장하면:
```
org.springframework.orm.ObjectOptimisticLockingFailureException:
  Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect):
  [com.pettrip.place.model.PlacePetPolicy#kakao-test-001]
Caused by: org.hibernate.StaleObjectStateException
  at DefaultMergeEventListener.entityIsDetached(...)
```

## 실패 원인
Spring Data JPA의 `SimpleJpaRepository.save()`는 `isNew()` 판별로 `persist` vs `merge`를 결정한다.
- UUID PK: 값이 없으면(`null`) → `persist`
- `@GeneratedValue`: DB 채번 전 → `persist`
- **수동 할당 String PK (또는 `@MapsId` 공유 PK)**: 저장 전부터 ID가 이미 설정되어 있음 → JPA가 `기존 엔티티`로 오인 → `merge` 시도 → DB에 없어 `StaleObjectStateException`

`PlacePetPolicy`는 `places.external_place_id`(VARCHAR)를 `@MapsId`로 공유하므로 생성자에서 ID가 즉시 할당된다. 따라서 `save()` 시점에 ID가 존재해 `merge()`가 호출된다.

## 해결책 (현재 적용)
`PlacePetPolicy`가 `Persistable<String>`을 구현하여 `isNew()` 판별 로직을 직접 제공:

```java
public class PlacePetPolicy implements Persistable<String> {

  @Transient private boolean isNew = true;

  @PostPersist
  @PostLoad
  void markNotNew() { this.isNew = false; }

  @Override public String getId() { return externalPlaceId; }
  @Override public boolean isNew() { return isNew; }
}
```
- 처음 생성 시 `isNew = true` → `persist`
- `@PostPersist`/`@PostLoad` 이후 `false` → 이후 `merge`

## 에이전트 행동 지침
- **수동 할당 PK(`String`, `Long` 등)를 갖는 엔티티**를 Spring Data JPA `save()`로 저장할 때는 반드시 `Persistable<T>` 구현을 추가하라.
- `@MapsId`로 부모 PK를 공유하는 자식 엔티티도 동일하게 적용하라.
- `@GeneratedValue`나 `BaseEntity`(UUIDv7 자동 생성)를 쓰는 엔티티에는 불필요하다 — ID가 미할당 상태에서 시작하므로 JPA가 `isNew`를 정상 감지한다.
