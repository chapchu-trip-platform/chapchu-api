# 004. 인증·저장소·임베딩 방식 확정

## 상태
- [x] 확정됨 (Accepted)

## 결정
- **인증**: Google OAuth 2.0 + JWT (Stateless, Spring Security)
- **파일 저장소**: AWS S3 (스탬프 사진, 리뷰 사진, 커뮤니티 사진)
- **임베딩 모델**: OpenAI `text-embedding-3-large` (차원 3072, 추천 품질 우선)
- **벡터 DB**: PostgreSQL + pgvector (단일 DB로 RDB + 벡터 통합)
- **LLM**: Anthropic Claude (Spring AI Anthropic 스타터)
- **RAG 프레임워크**: Spring AI

## 에이전트 행동 지침
- 사진 URL은 S3 경로로 저장하라. 로컬 파일 경로를 DB에 저장하지 마라.
- JWT 토큰 발급·검증 로직은 `user.service` 레이어에 위치시켜라.
- 벡터 임베딩 생성은 Spring AI `EmbeddingModel` 인터페이스를 사용하라.
- pgvector 임베딩 컬럼은 `vector(3072)`으로 선언하라. 차원 수는 상수(`EMBEDDING_DIMENSION = 3072`)로 추출해 한 곳에서 관리하라.
