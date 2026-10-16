# 009. pgvector HNSW 인덱스 — 3072차원 임베딩과 충돌 (앱 기동 자체 실패)

## 증상
Flyway 마이그레이션(`V1__init_schema.sql`) 적용 시 `./gradlew :app:bootRun` 또는 CI가 아래 오류로 전체 실패:
```
FlywaySqlScriptException: Script V1__init_schema.sql failed
ERROR: column cannot have more than 2000 dimensions for hnsw index
```
pet/user 등 특정 도메인과 무관하게, **로컬/CI 어디서든 DB가 깨끗한 상태에서 마이그레이션을 실행하면 앱이 아예 기동되지 않는다.**

## 실패 원인
- `docs/decisions/004-auth-storage-embedding.md`에서 임베딩 모델을 `text-embedding-3-large`(3072차원)로 확정.
- `V1__init_schema.sql`이 `course_embeddings.embedding`, `place_embeddings.embedding` 컬럼(`vector(3072)`)에 다음 인덱스를 생성:
  ```sql
  CREATE INDEX idx_course_embeddings_hnsw ON course_embeddings USING hnsw (embedding vector_cosine_ops);
  CREATE INDEX idx_place_embeddings_hnsw ON place_embeddings USING hnsw (embedding vector_cosine_ops);
  ```
- pgvector의 **HNSW와 IVFFlat 모두 최대 2000차원**까지만 지원한다. 3072차원 컬럼에는 두 인덱스 타입 모두 사용 불가.

## 해결 (2026-07-28, MVP 단계)
V1에서 HNSW 인덱스 생성 구문 제거, 시퀀셜 스캔으로 운영.

V1이 어떤 환경에서도 한 번도 성공 적용된 적 없어 Flyway 체크섬 이슈 없음.

```sql
-- 제거된 라인 (V1__init_schema.sql 394~395번)
-- CREATE INDEX idx_course_embeddings_hnsw ON course_embeddings USING hnsw (embedding vector_cosine_ops);
-- CREATE INDEX idx_place_embeddings_hnsw ON place_embeddings USING hnsw (embedding vector_cosine_ops);
```

## 향후 대응 후보 (임베딩 담당자 합의 필요)
1. **차원 축소**: `text-embedding-3-large`의 `dimensions` 파라미터로 1536 또는 1024로 축소 → HNSW/IVFFlat 사용 가능
2. **halfvec 타입** (pgvector 0.7+): 4000차원까지 HNSW 지원, 스키마 변경 필요
3. **외부 벡터 DB**: Pinecone, Weaviate 등으로 임베딩 분리

결정되면 V2 마이그레이션으로 인덱스 추가.

## 에이전트 행동 지침
- 벡터 인덱스 없음 = 정상 상태 (MVP). 임의로 인덱스 추가하지 마라.
- 임베딩 관련 스키마 변경은 담당자 합의 후 V2+ 마이그레이션으로만 진행.
