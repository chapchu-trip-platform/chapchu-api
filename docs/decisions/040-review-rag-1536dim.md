# 040 — 리뷰 임베딩: text-embedding-3-small(1536차원) + HNSW 인덱스

## 결정

리뷰 작성 시 날씨·별점·내용을 조합한 텍스트를 OpenAI `text-embedding-3-small`로 임베딩(1536차원)하여
`review_embeddings` 테이블에 저장한다. HNSW 인덱스를 적용해 ANN 검색을 지원한다.

## 이유

- 기존 `place_embeddings`·`course_embeddings`는 `vector(3072)`(text-embedding-3-large)로 설계되었으나
  HNSW·IVFFlat 모두 2000차원 한계로 인덱스 적용 불가(failures/009).
- 리뷰 임베딩은 신규 테이블이므로 1536차원으로 시작, HNSW 인덱스를 즉시 적용 가능.
- text-embedding-3-small과 large 품질 차이는 약 3-5%이나, 짧은 리뷰 텍스트에서는 실질 차이 미미.
- 비용: text-embedding-3-small = $0.02/1M 토큰 (large 대비 약 1/6).

## 임베딩 텍스트 포맷

```
날씨: {weather or '정보없음'}
별점: {rating}점
리뷰: {contents}
```

## 설계 의도 (의도적 결정)

- **`createReview`는 `@Transactional` 없음** — `reviewRepository.save()`는 Spring Data 자체 트랜잭션으로
  원자적으로 커밋된다. 임베딩 호출은 그 이후에 실행되어 DB 커넥션을 점유하지 않는다.
  향후 createReview에 여러 write 작업이 추가되면 `@Transactional`을 복원해야 한다.

- **임베딩 실패 시 리뷰 저장 유지** — OpenAI API 장애가 사용자 리뷰 작성을 막지 않는다.
  임베딩이 없는 리뷰는 DB에는 정상 존재하지만 RAG 검색 대상에서 누락된다.
  실패 시 ERROR 로그(`reviewId` 포함)를 남겨 운영 중 식별 가능하게 한다.
  재처리(백필) 잡은 이슈로 관리한다(#145).

- **INSERT 멱등성** — `ON CONFLICT (review_id) DO UPDATE`로 재처리 재호출 시 덮어쓰기.

## 관련 파일

- `module-review/service/ReviewEmbeddingService.java` — 임베딩 생성·저장 (JdbcTemplate + `?::vector` 캐스트)
- `V15__create_review_embeddings.sql` — DDL + HNSW 인덱스
- `application.yml` — `dimensions: 1536` 명시로 모델 변경 시 불일치 조기 검출

## 참고

- failures/009: HNSW 2000차원 한계
- decisions/004: place/course 임베딩은 text-embedding-3-large 유지 (별도 마이그레이션 필요 시 협의)
