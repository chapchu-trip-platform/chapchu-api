-- comments.deleted_at — 댓글 소프트 삭제 (이슈 #175)
--
-- parent_comment_id가 ON DELETE CASCADE라서 부모 댓글을 지우면 대댓글이 통째로
-- 사라졌다. 대댓글을 쓴 사람 입장에서는 자기 글이 예고 없이 없어지는 셈이다.
--
-- 행을 남기고 deleted_at만 찍는다. 조회할 때 내용과 닉네임을 가리면
-- 스레드 구조가 유지되고 posts.comment_count도 1씩만 줄어든다.
--
-- IF NOT EXISTS: 개발 DB에 손으로 먼저 적용했을 수 있다.
ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

COMMENT ON COLUMN comments.deleted_at IS '삭제 시각. NULL이면 살아 있는 댓글';
