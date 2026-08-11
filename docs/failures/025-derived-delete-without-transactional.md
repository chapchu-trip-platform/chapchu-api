# 025. 파생 delete 쿼리에 @Transactional을 빠뜨려 북마크 취소가 항상 500

## 증상

`DELETE /posts/{postId}/bookmarks`가 **항상** 500을 낸다. 북마크 등록(201)과 중복 방지(409)는 정상이라
겉보기에는 기능이 완성돼 있다.

```
InvalidDataAccessApiUsageException:
  No EntityManager with actual transaction available for current thread
  - cannot reliably process 'remove' call
    at com.pettrip.post.service.PostService.cancelBookmark(PostService.java:100)
```

## 원인

Spring Data JPA의 **파생 delete 쿼리**(`deleteByUserIdAndPostId` 같은 메서드)는 트랜잭션 안에서만 실행된다.
`SimpleJpaRepository`가 기본으로 붙여주는 `@Transactional`은 `save`, `delete(entity)` 같은 CRUD 메서드에만
적용되고, 우리가 인터페이스에 직접 선언한 파생 delete에는 붙지 않는다.

같은 클래스의 다른 메서드에는 있었다.

```java
@Transactional
public void cancelRecommendation(...) { ... deleteByPostIdAndUserId(...); }   // 있음

public void cancelBookmark(...) { ... deleteByUserIdAndPostId(...); }         // 없음 ← 여기
```

`WishlistService.removeFromWishlist`에도 있다. **한 군데만 빠졌다.**

## 왜 테스트가 못 잡았나

이게 핵심이다. 이 메서드에는 테스트가 세 개나 있었다.

| 테스트 | 방식 | 잡을 수 있었나 |
|---|---|---|
| `PostBookmarkControllerTest` | `@WebMvcTest` + 서비스 **mock** | 못 잡음 — 서비스가 가짜다 |
| `PostServiceTest` | Mockito + 리포지토리 **mock** | 못 잡음 — 리포지토리가 가짜라 트랜잭션이 필요 없다 |
| `PostRepositoryTest` | `@DataJpaTest` | 못 잡음 — `@DataJpaTest`가 테스트에 트랜잭션을 걸어준다 |

**셋 다 초록불인데 운영에서는 100% 실패한다.** 각 층은 자기 몫을 정확히 검증했지만, 트랜잭션 경계는
층과 층 **사이**에 있어서 아무도 보지 않았다.

## 해결

```java
/** 파생 delete 쿼리는 트랜잭션 안에서만 실행된다. 빠지면 호출이 통째로 500이 된다. */
@Transactional
public void cancelBookmark(UUID userId, UUID postId) { ... }
```

그리고 이런 종류를 앞으로 잡기 위해 통합 테스트를 도입했다 (`docs/decisions/031` 참고).
실제로 이 버그는 그 통합 테스트를 처음 돌린 실행에서 잡혔다.

## 에이전트 행동 지침

- 리포지토리 인터페이스에 **직접 선언한** `deleteBy...` / `removeBy...`를 호출하는 서비스 메서드에는
  반드시 `@Transactional`을 붙여라. `save`나 `delete(entity)`와 다르다.
- **mock을 쓰는 테스트는 트랜잭션 경계를 검증하지 못한다.** 리포지토리를 mock하면 트랜잭션이 없어도
  아무 일도 일어나지 않는다.
- `@DataJpaTest`는 테스트 메서드를 트랜잭션으로 감싸므로 **없는 트랜잭션을 있는 것처럼 보이게 한다.**
  이 조합에서 통과했다고 운영에서 도는 것이 아니다.
- 같은 클래스 안에서 비슷한 메서드끼리 애노테이션이 다르면 의심하라. 대개 빠뜨린 쪽이 틀렸다.
