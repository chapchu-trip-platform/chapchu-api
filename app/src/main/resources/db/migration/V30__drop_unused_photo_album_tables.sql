-- 미사용 사진/앨범 테이블 제거 (decisions/044).
-- albums·album_photos: 앨범은 review_photos 파생으로 동작(GET /users/me/album)하며 이 테이블은 미사용.
-- visit_verifications: 장소인증이 FE 전담으로 이관되어 백엔드에서 불필요.
-- 세 테이블 모두 Java 코드·엔티티·엔드포인트 참조 0건, FK상 leaf라 순서만 지키면 안전하게 DROP.
DROP TABLE IF EXISTS album_photos;
DROP TABLE IF EXISTS albums;
DROP TABLE IF EXISTS visit_verifications;
