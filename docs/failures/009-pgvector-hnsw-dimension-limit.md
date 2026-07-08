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
- pgvector의 HNSW 인덱스는 **최대 2000차원**까지만 지원한다 (IVFFlat도 2000차원 제한 동일). 3072차원 컬럼에는 두 인덱스 타입 모두 그대로 사용 불가.

## 해결책
**미해결 — 팀 논의 필요.** 후보:
1. 임베딩 차원을 2000 이하로 축소 (예: `text-embedding-3-large`를 `dimensions` 파라미터로 축소 요청, 또는 다른 모델/차원 축소 기법 사용)
2. HNSW/IVFFlat 인덱스 없이 시퀀셜 스캔으로 운영 (초기 데이터량이 적은 MVP 단계에서는 허용 가능할 수 있음) — 인덱스 생성 구문만 제거
3. pgvector 0.7.0+의 `halfvec` 타입(2000차원 제한이 실차원의 2배인 4000까지 허용) 등 대안 타입 검토

## 에이전트 행동 지침
- `docs/decisions/011`(Flyway) 규칙상 기존 마이그레이션 파일(`V1__init_schema.sql`)을 임의로 수정하지 마라 — 체크섬 검증으로 타 팀원 환경이 깨질 수 있다.
- 이 문제의 해결책은 `V2` 이후 신규 마이그레이션(인덱스 재생성) + 관련 `docs/decisions/004` 갱신 형태로, 담당자(recommendation/임베딩 도메인)와 합의 후 진행하라.
- 이 파일이 존재하는 한 **로컬에서 깨끗한 DB로 앱을 처음 기동하면 무조건 실패한다** — 온보딩 중 이 오류를 만나면 새로운 버그가 아니라 이미 알려진 이슈임을 인지하라.
