-- 장소 위시리스트. 사용자가 찜한 장소 목록 (user_id, place_id) 복합 PK.
CREATE TABLE place_wishlists (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    place_id   VARCHAR(255) NOT NULL REFERENCES places(external_place_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (user_id, place_id)
);

COMMENT ON TABLE place_wishlists IS '장소 위시리스트. N:M junction table';
COMMENT ON COLUMN place_wishlists.user_id IS '찜한 사용자. FK → users';
COMMENT ON COLUMN place_wishlists.place_id IS '찜한 장소. FK → places.external_place_id';
