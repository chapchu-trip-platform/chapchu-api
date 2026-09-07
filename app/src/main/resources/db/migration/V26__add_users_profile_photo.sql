-- 프로필 사진(프사) 연결. 사진은 photos에 업로드(type=PROFILE)한 뒤 그 photo_id를 참조한다.
-- 사진 없이도 가입/사용 가능하므로 nullable.
ALTER TABLE users ADD COLUMN profile_photo_id UUID REFERENCES photos(photo_id) ON DELETE SET NULL;
