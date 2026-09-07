-- 리뷰(후기)에 사진 여러 장을 매다는 조인 테이블. album_photos(V1)와 동일한 패턴.
-- 리뷰 1개 ↔ 사진 N개. 코스엔 리뷰가 여러 개 달리므로 코스당 사진이 여러 장 쌓인다.
-- 앨범·추천장소는 이 review_photos를 통해 리뷰 사진을 가져다 쓴다(후속).
CREATE TABLE review_photos (
    review_photo_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID NOT NULL REFERENCES reviews(review_id) ON DELETE CASCADE,
    photo_id        UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    photo_order     SMALLINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_review_photos_review_id ON review_photos(review_id);
