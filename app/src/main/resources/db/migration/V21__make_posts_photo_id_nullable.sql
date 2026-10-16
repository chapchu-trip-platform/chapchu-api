-- posts.photo_id NOT NULL 해제 — 사진 없는 게시글 허용 (이슈 #188)
--
-- 사진을 붙이려면 POST /photos/upload-url → S3 업로드 → POST /photos를 거쳐야 하고
-- 그 POST /photos가 다시 coursePlaceId를 요구한다. 사진 없이 글만 쓰는 경로가
-- API에 없어서 프론트가 POST /posts에서 400을 맞고 있었다.
--
-- FK는 그대로 둔다. NULL은 참조 무결성 검사를 건너뛴다.
-- DROP NOT NULL은 카탈로그만 바꾸는 즉시 연산이라 테이블 재작성이 없다.
ALTER TABLE posts
    ALTER COLUMN photo_id DROP NOT NULL;

COMMENT ON COLUMN posts.photo_id IS '대표 사진. NULL이면 사진 없는 게시글';
