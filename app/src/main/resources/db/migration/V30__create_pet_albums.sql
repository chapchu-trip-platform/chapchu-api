-- 앨범 + 추억앨범 (이슈 #263)
--
-- 앨범은 유저당 하나다. 사진마다 어느 반려동물인지를 달아 두고, 불러올 때 그 아이의
-- is_die로 두 묶음으로 가른다.
--   is_die = FALSE  ->  앨범
--   is_die = TRUE   ->  추억앨범
--
-- albums에 타입 컬럼을 두지 않는 이유: 두 값이 어긋나는 상태("추억앨범인데 그 아이는
-- 살아있음")를 막을 방법이 없다. is_die를 켜면 그 아이의 사진들이 자동으로 추억앨범
-- 쪽으로 옮겨가고, 사진을 실제로 이동시키는 작업이 필요 없다.

ALTER TABLE pets
    ADD COLUMN IF NOT EXISTS is_die BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN pets.is_die IS '무지개다리를 건넜는지. TRUE면 이 아이의 사진은 추억앨범으로 분류된다';

-- ── 앨범은 유저당 하나 ────────────────────────────────────────────────
ALTER TABLE albums
    ADD CONSTRAINT uq_albums_user_id UNIQUE (user_id);

CREATE INDEX IF NOT EXISTS idx_albums_user_id ON albums(user_id);

-- ── 사진이 어느 아이의 것인지 ─────────────────────────────────────────
-- 이 값이 없으면 앨범/추억앨범을 가를 수 없다. 사진을 담을 때 반드시 지정한다.
ALTER TABLE album_photos
    ADD COLUMN IF NOT EXISTS pet_id UUID REFERENCES pets(pet_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS photo_order SMALLINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_album_photos_album_id ON album_photos(album_id);
CREATE INDEX IF NOT EXISTS idx_album_photos_pet_id ON album_photos(pet_id);

COMMENT ON COLUMN album_photos.pet_id IS '사진 속 반려동물. 이 아이의 is_die로 앨범/추억앨범이 갈린다';
COMMENT ON COLUMN album_photos.photo_order IS '앨범 내 표시 순서';

-- ── 기존 유저에 앨범 백필 ─────────────────────────────────────────────
INSERT INTO albums (album_id, user_id, album_name, created_at, updated_at)
SELECT gen_random_uuid(), u.user_id, '내 앨범', now(), now()
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM albums a WHERE a.user_id = u.user_id);
