-- 반려동물 프로필 사진 + 배경화면 (이슈 #279)
--
-- 사진은 photos에 업로드(type=PROFILE)한 뒤 그 photo_id를 참조한다.
-- users.profile_photo_id(V26)와 같은 방식이고, 배경화면도 같은 경로에 저장한다.
--
-- 사진 없이도 등록·사용이 가능하므로 둘 다 nullable이다. 기본 이미지는 프론트가 처리하므로
-- 서버는 사진이 없으면 null을 내려준다.
--
-- ON DELETE SET NULL: 사진이 지워져도 반려동물 레코드는 살아 있어야 한다.
ALTER TABLE pets
    ADD COLUMN profile_photo_id    UUID REFERENCES photos(photo_id) ON DELETE SET NULL,
    ADD COLUMN background_photo_id UUID REFERENCES photos(photo_id) ON DELETE SET NULL;

COMMENT ON COLUMN pets.profile_photo_id IS '프로필 사진. FK → photos. NULL이면 사진 없음';
COMMENT ON COLUMN pets.background_photo_id IS '프로필 배경화면. FK → photos. NULL이면 사진 없음';
