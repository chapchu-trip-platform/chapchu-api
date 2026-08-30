# 042 — place_embeddings, course_embeddings 테이블 제거

## 결정

`place_embeddings`, `course_embeddings` 두 테이블을 DROP한다.

## 배경

V1 초기 스키마에서 RAG 파이프라인 확장을 염두에 두고 두 테이블을 미리 생성했다.
그러나 이후 실제 RAG 구현 과정에서 두 테이블이 쓸모없음이 확인됐다.

## 제거 근거

| 항목 | 상태 |
|---|---|
| Java 코드 참조 | 0건 |
| Flyway 마이그레이션(V2~V16) 참조 | 0건 |
| Entity / Repository / Service | 없음 |
| 다른 테이블의 FK 참조 | 없음 |
| 벡터 차원 | vector(3072) — pgvector HNSW 인덱스 한계(2000차원) 초과로 인덱스 불가 |
| 앱 임베딩 모델 | text-embedding-3-small (1536차원) — 3072차원 생성 수단 없음 |
| 데이터 | EMPTY |

## 설계 오류 분석

- 모델 선택(3072차원) → 스키마 설계 순서로 진행하다 pgvector 인덱스 한계 미검증
- 장소 유사도: 정적 메타데이터 임베딩보다 review_embeddings(실제 방문 경험)가 더 우월한 신호
- 코스 유사도: 자유텍스트 임베딩보다 구조적 유사도(장소 집합 Jaccard, 지역/테마 일치)가 적합

## 대체

- 장소 랭킹 → `review_embeddings`(1536차원, HNSW 인덱스 정상) + PlaceRagService로 커버
- 코스 유사도 → 향후 필요 시 임베딩이 아닌 구조적 유사도로 설계

## 관련

- 이슈: #154
- 실패 기록: docs/failures/009-pgvector-hnsw-dimension-limit.md
- 참고: docs/decisions/040-review-rag-1536dim.md
