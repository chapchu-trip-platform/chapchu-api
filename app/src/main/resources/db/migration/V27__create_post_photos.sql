-- 게시글에 사진 여러 장을 매다는 조인 테이블. review_photos(V25)와 동일한 패턴.
-- 게시글 1개 ↔ 사진 N개. 기존 posts.photo_id(단일 대표사진)는 백필 후 남겨둔다(비파괴).
CREATE TABLE post_photos (
    post_photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id       UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    photo_id      UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    photo_order   SMALLINT NOT NULL DEFAULT 0,
    created_at    TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_post_photos_post_id ON post_photos(post_id);

-- 기존 단일 사진을 post_photos로 이관. photo_id 컬럼 자체는 유지한다.
INSERT INTO post_photos (post_id, photo_id, photo_order)
SELECT post_id, photo_id, 0
FROM posts
WHERE photo_id IS NOT NULL;
