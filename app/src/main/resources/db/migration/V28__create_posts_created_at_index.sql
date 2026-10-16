-- 게시글 목록 정렬/필터용 인덱스.
-- 최신순(latest): ORDER BY created_at DESC, post_id DESC + 커서 범위 조건
-- 핫게(popular): WHERE created_at >= now() - 7일 (최근 구간 범위 스캔)
-- 둘 다 created_at 선두 컬럼을 쓰므로 하나로 커버한다.
CREATE INDEX idx_posts_created_at ON posts (created_at DESC, post_id DESC);
