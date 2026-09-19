-- 게시글 분류: 일반(GENERAL) / 여행후기(TRAVEL_REVIEW)
-- 기존 글은 전부 일반글로 본다. NOT NULL + DEFAULT라 기존 행도 채워진다.
ALTER TABLE posts ADD COLUMN post_type VARCHAR(20) NOT NULL DEFAULT 'GENERAL';

COMMENT ON COLUMN posts.post_type IS '글 종류 (GENERAL=일반, TRAVEL_REVIEW=여행후기)';

-- 목록은 타입 필터 + 최신순(created_at, post_id) 정렬로 조회한다.
-- V28의 created_at 인덱스는 타입 필터가 붙으면 쓰이지 않아 복합 인덱스를 따로 둔다.
CREATE INDEX idx_posts_type_created_at ON posts (post_type, created_at DESC, post_id DESC);
