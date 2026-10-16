-- place_embeddings, course_embeddings 제거
-- 이유: 코드 참조 0건, vector(3072)로 pgvector HNSW 인덱스 불가(한계 2000차원),
--       앱 임베딩 모델(text-embedding-3-small, 1536차원)로 생성 불가, EMPTY
-- 대체: 장소 랭킹은 review_embeddings(1536차원)로 충분히 커버됨
-- 결정: docs/decisions/042-drop-unused-embedding-tables.md 참고

DROP TABLE IF EXISTS place_embeddings;
DROP TABLE IF EXISTS course_embeddings;
