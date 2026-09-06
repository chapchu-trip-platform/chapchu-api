-- 사진을 방문 인증(코스 장소)뿐 아니라 게시글·앨범 등 여러 기능에서 업로드할 수 있도록
-- course_place_id 종속을 해제한다. 각 기능은 posts.photo_id, album_photos.photo_id,
-- visit_verifications.photo_id 등 자기 참조로 사진을 연결하므로, photos 자체는
-- "업로드된 이미지" 레코드로만 남긴다.
ALTER TABLE photos ALTER COLUMN course_place_id DROP NOT NULL;
